/* Hidden GOA-2026-Recap: 5x schneller Klick auf das Logo (nur auf der
   Archiv-Seite von 2026) blendet ein Video-Recap mit dem Line-Up ein. */
document.addEventListener('DOMContentLoaded', function () {
    if (window.location.pathname !== '/goa/2026') return;

    var brand = document.querySelector('.brand');
    if (!brand) return;

    var VIDEO_SRC = '/videos/goa2026-recap.mp4';
    var SLIDE_MS = 2800;

    var clicks = 0;
    var resetTimer = null;
    var overlay = null;
    var slideTimer = null;

    brand.addEventListener('click', function (event) {
        event.preventDefault();
        clicks += 1;
        clearTimeout(resetTimer);

        if (clicks >= 5) {
            clicks = 0;
            openRecap();
            return;
        }

        resetTimer = setTimeout(function () {
            clicks = 0;
            window.location.href = brand.getAttribute('href') || '/';
        }, 550);
    });

    function collectBands() {
        var cards = document.querySelectorAll('.band-card');
        var bands = [];
        cards.forEach(function (card) {
            var img = card.querySelector('img');
            var name = card.querySelector('.band-card-name');
            if (!name) return;
            bands.push({
                photo: img ? img.src : '',
                name: name.textContent.trim(),
                performance: card.getAttribute('data-performance') || ''
            });
        });
        return bands;
    }

    function escapeHtml(str) {
        var div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    function buildSlides() {
        var slides = collectBands().map(function (band) {
            return {
                photo: band.photo,
                title: band.name,
                sub: band.performance ? 'GESPIELT AM ' + band.performance.toUpperCase() : 'WAR DABEI'
            };
        });
        slides.push({
            photo: '',
            title: 'GOA 2026',
            sub: 'DANKE FÜRS FEIERN 🔥 BIS ZUM NÄCHSTEN MAL',
            finale: true
        });
        return slides;
    }

    function renderSlide(card, slide) {
        card.innerHTML =
            '<div class="egg-slide">' +
            (slide.photo ? '<img class="egg-photo" src="' + slide.photo + '" alt="">' : '') +
            '<div class="egg-name' + (slide.finale ? ' egg-finale' : '') + '">' + escapeHtml(slide.title) + '</div>' +
            '<div class="egg-sub">' + escapeHtml(slide.sub) + '</div>' +
            '</div>';
        var inner = card.querySelector('.egg-slide');
        requestAnimationFrame(function () {
            inner.classList.add('egg-pop');
        });
    }

    function onKeydown(event) {
        if (event.key === 'Escape') closeRecap();
    }

    function closeRecap() {
        if (!overlay) return;
        clearInterval(slideTimer);
        document.removeEventListener('keydown', onKeydown);
        document.body.classList.remove('egg-open');
        overlay.remove();
        overlay = null;
    }

    function openRecap() {
        if (overlay) return;
        var slides = buildSlides();

        overlay = document.createElement('div');
        overlay.className = 'egg-overlay';
        overlay.innerHTML =
            '<video class="egg-video" loop playsinline></video>' +
            '<div class="egg-scrim"></div>' +
            '<button type="button" class="egg-close" aria-label="Schließen">&times;</button>' +
            '<div class="egg-card"></div>';
        document.body.appendChild(overlay);
        document.body.classList.add('egg-open');

        var video = overlay.querySelector('.egg-video');
        video.src = VIDEO_SRC;
        video.play().catch(function () {
            video.muted = true;
            video.play().catch(function () {});
        });

        var card = overlay.querySelector('.egg-card');
        var index = 0;
        renderSlide(card, slides[index]);

        slideTimer = setInterval(function () {
            index += 1;
            if (index >= slides.length) {
                closeRecap();
                return;
            }
            renderSlide(card, slides[index]);
        }, SLIDE_MS);

        overlay.querySelector('.egg-close').addEventListener('click', closeRecap);
        overlay.querySelector('.egg-scrim').addEventListener('click', closeRecap);
        document.addEventListener('keydown', onKeydown);
    }
});
