/*
 * Copyright 2012 gitblit.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gitblit;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GitblitTrustManager is a wrapper trust manager that hot-reloads a local file
 * CRL and enforces client certificate revocations.  The GitblitTrustManager
 * also implements fuzzy revocation enforcement in case of issuer mismatch BUT
 * serial number match.  These rejections are specially noted in the log.
 *
 * @author James Moger
 */
public class GitblitTrustManager implements X509TrustManager {

    private static final Logger logger = LoggerFactory.getLogger(GitblitTrustManager.class);

    private final Set<X509TrustManager> delegates;
    private final File caRevocationList;

    private final AtomicLong lastModified = new AtomicLong(0);
    private volatile X509CRL crl;

    public GitblitTrustManager(File crlFile, TrustManager[]... trustManagers) {
        this.delegates = Arrays.stream(trustManagers)
                .flatMap(Arrays::stream)
                .filter(c -> c instanceof X509TrustManager)
                .map(c -> (X509TrustManager) c)
                .collect(Collectors.toUnmodifiableSet());
        this.caRevocationList = crlFile;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        X509Certificate cert = chain[0];
        if (isRevoked(cert)) {
            String message = MessageFormat.format("Rejecting revoked certificate {0,number,0} for {1}",
                    cert.getSerialNumber(), cert.getSubjectDN().getName());
            logger.warn(message);
            throw new CertificateException(message);
        }
        CertificateException lastError = null;
        for (final X509TrustManager trustManager : delegates) {
            try {
                trustManager.checkClientTrusted(chain, authType);
                return;
            } catch (CertificateException ex) {
                lastError = ex;
            }
        }

        if (lastError != null) {
            throw new CertificateException(
                    "Cannot find suitable client trusted certificate among " + delegates.size() + " trust managers", lastError);
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        CertificateException lastError = null;
        for (final X509TrustManager trustManager : delegates) {
            try {
                trustManager.checkServerTrusted(chain, authType);
                return;
            } catch (CertificateException ex) {
                lastError = ex;
            }
        }

        if (lastError != null) {
            throw new CertificateException(
                    "Cannot find suitable server trusted certificate among " + delegates.size() + " trust managers", lastError);
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return this.delegates.stream()
                .map(X509TrustManager::getAcceptedIssuers)
                .flatMap(Arrays::stream)
                .toList()
                .toArray(new X509Certificate[]{});
    }

    protected boolean isRevoked(X509Certificate cert) {
        if (!caRevocationList.exists()) {
            return false;
        }
        read();

        if (crl.isRevoked(cert)) {
            // exact cert is revoked
            return true;
        }

        X509CRLEntry entry = crl.getRevokedCertificate(cert.getSerialNumber());
        if (entry != null) {
            logger.warn("Certificate issuer does not match CRL issuer, but serial number has been revoked!");
            logger.warn("   cert issuer = " + cert.getIssuerX500Principal());
            logger.warn("   crl issuer  = " + crl.getIssuerX500Principal());
            return true;
        }

        return false;
    }

    protected synchronized void read() {
        if (lastModified.get() == caRevocationList.lastModified()) {
            return;
        }
        logger.info("Reloading CRL from " + caRevocationList.getAbsolutePath());
        InputStream inStream = null;
        try {
            inStream = new FileInputStream(caRevocationList);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509CRL list = (X509CRL) cf.generateCRL(inStream);
            crl = list;
            lastModified.set(caRevocationList.lastModified());
        } catch (Exception e) {
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (Exception e) {
                }
            }
        }
    }
}