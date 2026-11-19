(function () {
    function dismiss(message) {
        if (!message || message.classList.contains("is-hiding")) {
            return;
        }

        window.clearTimeout(message.dismissTimer);
        message.classList.add("is-hiding");
        window.setTimeout(() => message.remove(), 280);
    }

    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll(".flash-message").forEach((message) => {
            const duration = Number(message.dataset.autoDismiss) || 5000;
            message.dismissTimer = window.setTimeout(() => dismiss(message), duration);

            const closeButton = message.querySelector("[data-dismiss-flash]");
            if (closeButton) {
                closeButton.addEventListener("click", () => dismiss(message));
            }
        });
    });
})();
