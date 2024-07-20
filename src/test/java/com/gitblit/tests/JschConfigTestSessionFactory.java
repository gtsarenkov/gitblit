package com.gitblit.tests;

import java.security.KeyPair;

import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory;
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig;
import org.eclipse.jgit.util.FS;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class JschConfigTestSessionFactory extends JschConfigSessionFactory {

	final KeyPair keyPair;

	public JschConfigTestSessionFactory(KeyPair keyPair) {
		this.keyPair = keyPair;
	}

    @Override
    protected void configure(OpenSshConfig.Host host, Session session) {
        session.setConfig("StrictHostKeyChecking", "no");
    }

    @Override
	protected JSch getJSch(final OpenSshConfig.Host hc, FS fs) throws JSchException {
    	JSch jsch = super.getJSch(hc, fs);
//    	jsch.removeAllIdentity();
//    	jsch.addIdentity("unittest", keyPair.getPrivate().getEncoded(), keyPair.getPublic().getEncoded(), null);
    	return jsch;
    }
}