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

    // Hall of Fame: reveal each card with a fade/rise once it scrolls into view,
    // instead of everything just being there on load - falls back to showing
    // them all immediately if the browser has no IntersectionObserver.
    var hofCards = document.querySelectorAll('.hof-card-wrap');
    if (hofCards.length) {
        if ('IntersectionObserver' in window) {
            var hofObserver = new IntersectionObserver(function (entries) {
                entries.forEach(function (entry) {
                    if (entry.isIntersecting) {
                        entry.target.classList.add('in-view');
                        hofObserver.unobserve(entry.target);
                    }
                });
            }, { threshold: 0.15, rootMargin: '0px 0px -40px 0px' });
            hofCards.forEach(function (card) { hofObserver.observe(card); });
        } else {
            hofCards.forEach(function (card) { card.classList.add('in-view'); });
        }
    }

    // On devices with a native share sheet (Android/iOS), let the share-card
    // button hand the PNG to navigator.share instead of just downloading it -
    // everywhere else it keeps acting as a plain download link.
    document.querySelectorAll('.band-detail-share-btn').forEach(function (btn) {
        if (!(navigator.share && navigator.canShare)) return;

        var label = btn.querySelector('.share-btn-label');
        if (label) label.textContent = '📤 Teilen';

        btn.addEventListener('click', function (event) {
            event.preventDefault();
            var url = btn.getAttribute('href');
            var filename = btn.getAttribute('download') || 'share-card.png';
            var shareTitle = btn.getAttribute('data-share-title') || document.title;

            fetch(url)
                .then(function (response) { return response.blob(); })
                .then(function (blob) {
                    var file = new File([blob], filename, { type: blob.type || 'image/png' });
                    if (!navigator.canShare({ files: [file] })) {
                        throw new Error('file-sharing-unsupported');
                    }
                    return navigator.share({ files: [file], title: shareTitle });
                })
                .catch(function (error) {
                    if (error && error.name === 'AbortError') return;
                    window.open(url, '_blank', 'noopener');
                });
        });
    });

    // Fan-upload page: list the chosen files (name + size) so picking a batch of
    // photos/videos gives some confirmation before submitting, and disable the submit
    // button with a "please wait" label on submit - a large video can take a while to
    // upload and a plain form POST otherwise gives no feedback that anything is happening.
    var fanUploadInput = document.getElementById('fan-upload-input');
    var fanUploadFilelist = document.getElementById('fan-upload-filelist');
    if (fanUploadInput && fanUploadFilelist) {
        fanUploadInput.addEventListener('change', function () {
            fanUploadFilelist.innerHTML = '';
            Array.prototype.forEach.call(fanUploadInput.files, function (file) {
                var li = document.createElement('li');
                var sizeMb = (file.size / (1024 * 1024)).toFixed(1);
                li.textContent = file.name + ' (' + sizeMb + ' MB)';
                fanUploadFilelist.appendChild(li);
            });
        });
    }

    var fanUploadForm = document.getElementById('fan-upload-form');
    if (fanUploadForm) {
        fanUploadForm.addEventListener('submit', function () {
            var submitBtn = fanUploadForm.querySelector('.fan-upload-submit');
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.textContent = 'Wird hochgeladen – bei großen Videos kann das dauern...';
            }
        });
    }
});
