document.addEventListener('DOMContentLoaded', function () {
    var navToggle = document.querySelector('.nav-toggle');
    var navLinks = document.querySelector('.nav-links');
    if (navToggle && navLinks) {
        navToggle.addEventListener('click', function () {
            navLinks.classList.toggle('open');
        });
    }

    var faqItems = document.querySelectorAll('.faq-item');
    faqItems.forEach(function (item) {
        var button = item.querySelector('.faq-question');
        if (!button) return;
        button.addEventListener('click', function () {
            var wasOpen = item.classList.contains('open');
            faqItems.forEach(function (other) {
                other.classList.remove('open');
            });
            if (!wasOpen) {
                item.classList.add('open');
            }
        });
    });

    var historyDropdowns = document.querySelectorAll('.nav-history-dropdown');
    historyDropdowns.forEach(function (dropdown) {
        var toggle = dropdown.querySelector('.dropdown-toggle');
        if (!toggle) return;
        toggle.addEventListener('click', function (event) {
            event.stopPropagation();
            var wasOpen = dropdown.classList.contains('open');
            historyDropdowns.forEach(function (other) {
                other.classList.remove('open');
            });
            if (!wasOpen) {
                dropdown.classList.add('open');
            }
        });
    });
    document.addEventListener('click', function () {
        historyDropdowns.forEach(function (dropdown) {
            dropdown.classList.remove('open');
        });
    });

    document.querySelectorAll('[data-modal-open]').forEach(function (opener) {
        opener.addEventListener('click', function () {
            var modal = document.getElementById(opener.getAttribute('data-modal-open'));
            if (modal) modal.classList.add('open');
        });
    });
    document.querySelectorAll('.modal-overlay').forEach(function (overlay) {
        var closeBtn = overlay.querySelector('.modal-close');
        if (closeBtn) {
            closeBtn.addEventListener('click', function () {
                overlay.classList.remove('open');
            });
        }
        overlay.addEventListener('click', function (event) {
            if (event.target === overlay) overlay.classList.remove('open');
        });
    });
});
