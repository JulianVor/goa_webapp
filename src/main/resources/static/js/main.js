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
});
