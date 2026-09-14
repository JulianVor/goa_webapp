/* Hidden GOA-2025-Recap: Klick-und-halten aufs Logo (nur auf der
   Archiv-Seite von 2025) startet eine zufällig gemischte Foto-Diashow. */
document.addEventListener('DOMContentLoaded', function () {
    if (window.location.pathname !== '/goa/2025') return;

    var brand = document.querySelector('.brand');
    if (!brand) return;

    var PHOTO_COUNT = 192;
    var PHOTO_BASE = '/images/goa2025-recap/';
    var SLIDE_MS = 4500;
    var HOLD_MS = 1800;

    var holdTimer = null;
    var holdTriggered = false;
    var holdBar = document.createElement('span');
    holdBar.className = 'egg2-hold-bar';
    brand.appendChild(holdBar);
    brand.classList.add('egg2-hold-anchor');

    function startHold() {
        if (holdTimer || overlay) return;
        holdBar.classList.add('filling');
        holdTimer = setTimeout(function () {
            holdTriggered = true;
            resetHoldVisual();
            openSlideshow();
        }, HOLD_MS);
    }

    function resetHoldVisual() {
        holdBar.classList.remove('filling');
        void holdBar.offsetWidth;
    }

    function cancelHold() {
        clearTimeout(holdTimer);
        holdTimer = null;
        resetHoldVisual();
    }

    brand.addEventListener('mousedown', startHold);
    brand.addEventListener('touchstart', startHold, { passive: true });
    brand.addEventListener('mouseup', cancelHold);
    brand.addEventListener('mouseleave', cancelHold);
    brand.addEventListener('touchend', cancelHold);
    brand.addEventListener('touchmove', cancelHold);
    brand.addEventListener('click', function (event) {
        if (holdTriggered) {
            event.preventDefault();
            holdTriggered = false;
        }
    });

    var overlay = null;
    var order = [];
    var pos = 0;
    var playing = true;
    var autoTimer = null;
    var layerA = null;
    var layerB = null;
    var activeIsA = true;
    var touchStartX = null;

    function shuffledOrder() {
        var arr = [];
        for (var i = 1; i <= PHOTO_COUNT; i++) arr.push(i);
        for (var j = arr.length - 1; j > 0; j--) {
            var k = Math.floor(Math.random() * (j + 1));
            var tmp = arr[j];
            arr[j] = arr[k];
            arr[k] = tmp;
        }
        return arr;
    }

    function photoUrl(n) {
        return PHOTO_BASE + String(n).padStart(3, '0') + '.jpg';
    }

    function updateCounter() {
        var counter = overlay.querySelector('.egg2-counter');
        if (counter) counter.textContent = (pos + 1) + ' / ' + order.length;
    }

    function preloadNeighbors() {
        [1, 2].forEach(function (offset) {
            var idx = (pos + offset) % order.length;
            var img = new Image();
            img.src = photoUrl(order[idx]);
        });
    }

    function showCurrent() {
        var n = order[pos];
        var nextLayer = activeIsA ? layerB : layerA;
        var currentLayer = activeIsA ? layerA : layerB;
        nextLayer.onload = function () {
            nextLayer.classList.add('visible');
            currentLayer.classList.remove('visible');
            activeIsA = !activeIsA;
        };
        nextLayer.src = photoUrl(n);
        updateCounter();
        preloadNeighbors();
    }

    function goTo(newPos) {
        if (newPos >= order.length) {
            order = shuffledOrder();
            pos = 0;
        } else if (newPos < 0) {
            pos = order.length - 1;
        } else {
            pos = newPos;
        }
        showCurrent();
    }

    function next() { goTo(pos + 1); }
    function prev() { goTo(pos - 1); }

    function restartAutoplay() {
        clearInterval(autoTimer);
        if (playing) autoTimer = setInterval(next, SLIDE_MS);
    }

    function togglePlay() {
        playing = !playing;
        overlay.querySelector('.egg2-playpause').textContent = playing ? '⏸' : '▶';
        restartAutoplay();
    }

    function onKeydown(event) {
        if (event.key === 'Escape') closeSlideshow();
        if (event.key === 'ArrowLeft') { prev(); restartAutoplay(); }
        if (event.key === 'ArrowRight') { next(); restartAutoplay(); }
    }

    function onTouchStart(event) { touchStartX = event.touches[0].clientX; }

    function onTouchEnd(event) {
        if (touchStartX == null) return;
        var dx = event.changedTouches[0].clientX - touchStartX;
        if (Math.abs(dx) > 40) {
            if (dx < 0) next(); else prev();
            restartAutoplay();
        }
        touchStartX = null;
    }

    function closeSlideshow() {
        if (!overlay) return;
        clearInterval(autoTimer);
        document.removeEventListener('keydown', onKeydown);
        document.body.classList.remove('egg-open');
        overlay.remove();
        overlay = null;
    }

    function openSlideshow() {
        if (overlay) return;
        order = shuffledOrder();
        pos = 0;
        playing = true;
        activeIsA = true;

        overlay = document.createElement('div');
        overlay.className = 'egg2-overlay';
        overlay.innerHTML =
            '<button type="button" class="egg2-close" aria-label="Schließen">&times;</button>' +
            '<div class="egg2-counter"></div>' +
            '<div class="egg2-viewer">' +
                '<img class="egg2-img egg2-img-a" alt="">' +
                '<img class="egg2-img egg2-img-b" alt="">' +
            '</div>' +
            '<div class="egg2-controls">' +
                '<button type="button" class="egg2-prev" aria-label="Zurück">&#8249;</button>' +
                '<button type="button" class="egg2-playpause" aria-label="Pause">&#10074;&#10074;</button>' +
                '<button type="button" class="egg2-next" aria-label="Weiter">&#8250;</button>' +
            '</div>';
        document.body.appendChild(overlay);
        document.body.classList.add('egg-open');

        layerA = overlay.querySelector('.egg2-img-a');
        layerB = overlay.querySelector('.egg2-img-b');

        overlay.querySelector('.egg2-close').addEventListener('click', closeSlideshow);
        overlay.querySelector('.egg2-prev').addEventListener('click', function () { prev(); restartAutoplay(); });
        overlay.querySelector('.egg2-next').addEventListener('click', function () { next(); restartAutoplay(); });
        overlay.querySelector('.egg2-playpause').addEventListener('click', togglePlay);
        overlay.querySelector('.egg2-viewer').addEventListener('touchstart', onTouchStart, { passive: true });
        overlay.querySelector('.egg2-viewer').addEventListener('touchend', onTouchEnd);
        document.addEventListener('keydown', onKeydown);

        layerA.onload = function () { layerA.classList.add('visible'); };
        layerA.src = photoUrl(order[0]);
        updateCounter();
        preloadNeighbors();

        restartAutoplay();
    }
});
