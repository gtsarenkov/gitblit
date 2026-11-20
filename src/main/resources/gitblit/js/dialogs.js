document.addEventListener('click', function (event) {
    if (!(event.target instanceof Element)) {
        return;
    }

    const confirmation = event.target.closest('[data-confirm-message]');
    if (confirmation && !window.confirm(confirmation.dataset.confirmMessage)) {
        event.preventDefault();
        event.stopImmediatePropagation();
        return;
    }

    const prompt = event.target.closest('[data-prompt-message]');
    if (prompt) {
        window.prompt(prompt.dataset.promptMessage, prompt.dataset.promptValue || '');
    }
}, true);
