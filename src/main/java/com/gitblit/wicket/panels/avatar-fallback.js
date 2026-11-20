(function () {
    function showFallback(image) {
        const fallback = image.dataset.avatarFallback;
        if (fallback) {
            delete image.dataset.avatarFallback;
            image.src = fallback;
        }
    }

    document.addEventListener('error', function (event) {
        if (event.target instanceof HTMLImageElement) {
            showFallback(event.target);
        }
    }, true);

    document.querySelectorAll('img[data-avatar-fallback]').forEach(function (image) {
        if (image.complete && image.naturalWidth === 0) {
            showFallback(image);
        }
    });
}());
