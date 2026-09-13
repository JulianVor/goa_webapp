document.addEventListener('DOMContentLoaded', function () {
    var navToggle = document.querySelector('.nav-toggle');
    var navLinks = document.querySelector('.nav-links');
    if (navToggle && navLinks) {
        navToggle.addEventListener('click', function () {
            navLinks.classList.toggle('open');
        });
    }

    document.querySelectorAll('.faq-question').forEach(function (button) {
        button.addEventListener('click', function () {
            button.parentElement.classList.toggle('open');
        });
    });
});
