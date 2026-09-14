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
    var tickTimer = null;
    var slides = [];
    var index = 0;

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
            var iso = card.getAttribute('data-performance-iso') || '';
            var date = iso ? new Date(iso) : null;
            if (date && isNaN(date.getTime())) date = null;
            bands.push({
                photo: img ? img.src : '',
                name: name.textContent.trim(),
                performance: card.getAttribute('data-performance') || '',
                date: date
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
        var slidesList = collectBands().map(function (band) {
            return {
                photo: band.photo,
                title: band.name,
                sub: band.performance ? 'GESPIELT AM ' + band.performance.toUpperCase() : '',
                date: band.date
            };
        });
        slidesList.push({
            photo: '',
            title: 'GOA 2026',
            sub: 'DANKE FÜRS FEIERN 🔥 BIS ZUM NÄCHSTEN MAL',
            finale: true
        });
        return slidesList;
    }

    function formatElapsed(ms) {
        var totalSeconds = Math.max(0, Math.floor(ms / 1000));
        var totalDays = Math.floor(totalSeconds / 86400);
        var years = Math.floor(totalDays / 365);
        return {
            years: years,
            days: String(totalDays % 365).padStart(2, '0'),
            hours: String(Math.floor((totalSeconds % 86400) / 3600)).padStart(2, '0'),
            minutes: String(Math.floor((totalSeconds % 3600) / 60)).padStart(2, '0'),
            seconds: String(totalSeconds % 60).padStart(2, '0')
        };
    }

    function timeBlock(unit, value, label) {
        return '<div class="egg-time-block" data-unit="' + unit + '">' +
            '<div class="egg-time-value">' + value + '</div>' +
            '<div class="egg-time-label">' + label + '</div>' +
            '</div>';
    }

    function elapsedMarkup(date) {
        if (!date) return '';
        var elapsed = formatElapsed(Date.now() - date.getTime());
        var blocks = [];
        if (elapsed.years >= 1) {
            blocks.push(timeBlock('years', String(elapsed.years).padStart(2, '0'), 'JAHRE'));
        }
        blocks.push(timeBlock('days', elapsed.days, 'TAGE'));
        blocks.push(timeBlock('hours', elapsed.hours, 'STUNDEN'));
        blocks.push(timeBlock('minutes', elapsed.minutes, 'MINUTEN'));
        blocks.push(timeBlock('seconds', elapsed.seconds, 'SEKUNDEN'));

        var value = '<div class="egg-clock">' + blocks.join('<div class="egg-separator">:</div>') + '</div>';
        return '<div class="egg-elapsed">' + value + '</div><div class="egg-elapsed-label">HAT GESPIELT VOR</div>';
    }

    function renderSlide(stage, slide) {
        stage.innerHTML =
            '<div class="egg-slide">' +
            (slide.photo ? '<img class="egg-photo" src="' + slide.photo + '" alt="">' : '') +
            '<div class="egg-name' + (slide.finale ? ' egg-finale' : '') + '">' + escapeHtml(slide.title) + '</div>' +
            elapsedMarkup(slide.date) +
            (slide.sub ? '<div class="egg-sub">' + escapeHtml(slide.sub) + '</div>' : '') +
            '</div>';
        var inner = stage.querySelector('.egg-slide');
        requestAnimationFrame(function () {
            inner.classList.add('egg-pop');
        });
    }

    function setActiveThumb(thumbs, activeIndex) {
        thumbs.querySelectorAll('.egg-thumb').forEach(function (thumb) {
            thumb.classList.toggle('active', Number(thumb.getAttribute('data-index')) === activeIndex);
        });
    }

    function goToSlide(stage, thumbs, newIndex) {
        index = newIndex;
        renderSlide(stage, slides[index]);
        setActiveThumb(thumbs, index);
    }

    function onKeydown(event) {
        if (event.key === 'Escape') closeRecap();
    }

    function closeRecap() {
        if (!overlay) return;
        clearInterval(slideTimer);
        clearInterval(tickTimer);
        document.removeEventListener('keydown', onKeydown);
        document.body.classList.remove('egg-open');
        overlay.remove();
        overlay = null;
    }

    function openRecap() {
        if (overlay) return;
        slides = buildSlides();
        index = 0;

        overlay = document.createElement('div');
        overlay.className = 'egg-overlay';
        overlay.innerHTML =
            '<video class="egg-video" loop playsinline></video>' +
            '<div class="egg-scrim"></div>' +
            '<button type="button" class="egg-close" aria-label="Schließen">&times;</button>' +
            '<div class="egg-card"><div class="egg-stage"></div></div>' +
            '<div class="egg-thumbs"></div>';
        document.body.appendChild(overlay);
        document.body.classList.add('egg-open');

        var video = overlay.querySelector('.egg-video');
        video.src = VIDEO_SRC;
        video.play().catch(function () {
            video.muted = true;
            video.play().catch(function () {});
        });

        var stage = overlay.querySelector('.egg-stage');
        var thumbs = overlay.querySelector('.egg-thumbs');

        thumbs.innerHTML = slides
            .filter(function (slide) { return !slide.finale; })
            .map(function (slide, i) {
                var style = slide.photo ? ' style="background-image:url(\'' + slide.photo + '\')"' : '';
                return '<button type="button" class="egg-thumb" data-index="' + i + '" aria-label="' + escapeHtml(slide.title) + '"' + style + '></button>';
            })
            .join('');

        thumbs.querySelectorAll('.egg-thumb').forEach(function (thumb) {
            thumb.addEventListener('click', function () {
                clearInterval(slideTimer);
                goToSlide(stage, thumbs, Number(thumb.getAttribute('data-index')));
                slideTimer = setInterval(advance, SLIDE_MS);
            });
        });

        function advance() {
            var nextIndex = index + 1;
            if (nextIndex >= slides.length) {
                closeRecap();
                return;
            }
            goToSlide(stage, thumbs, nextIndex);
        }

        goToSlide(stage, thumbs, 0);
        slideTimer = setInterval(advance, SLIDE_MS);

        tickTimer = setInterval(function () {
            var current = slides[index];
            if (!current || !current.date) return;
            var stageElapsed = stage.querySelector('.egg-elapsed');
            if (!stageElapsed) return;
            renderElapsedInPlace(stage, current.date);
        }, 1000);

        function renderElapsedInPlace(stageEl, date) {
            var elapsed = formatElapsed(Date.now() - date.getTime());
            var units = {
                years: elapsed.years >= 1 ? String(elapsed.years).padStart(2, '0') : null,
                days: elapsed.days,
                hours: elapsed.hours,
                minutes: elapsed.minutes,
                seconds: elapsed.seconds
            };
            Object.keys(units).forEach(function (unit) {
                if (units[unit] == null) return;
                var el = stageEl.querySelector('.egg-time-block[data-unit="' + unit + '"] .egg-time-value');
                if (el) el.textContent = units[unit];
            });
        }

        overlay.querySelector('.egg-close').addEventListener('click', closeRecap);
        overlay.querySelector('.egg-scrim').addEventListener('click', closeRecap);
        document.addEventListener('keydown', onKeydown);
    }
});
