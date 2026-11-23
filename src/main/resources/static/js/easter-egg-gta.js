/* Hidden "GTA oder Harburg?" quiz: 5x schneller Klick auf das Logo auf der
   Hauptseite ("/") startet ein Rate-Quiz aus echten Harburger Polizeimeldungen
   und GTA: San-Andreas-Missionen - beide bewusst so neutral formuliert, dass
   man nur raten kann, welche Welt gemeint ist. */
document.addEventListener('DOMContentLoaded', function () {
    if (window.location.pathname !== '/') return;

    var brand = document.querySelector('.brand');
    if (!brand) return;

    var DATA_URL = '/quiz/gta-oder-harburg.json';
    var ROUND_SIZE = 5;

    var clicks = 0;
    var resetTimer = null;
    var overlay = null;
    var allCases = null;
    var round = [];
    var index = 0;
    var score = 0;

    brand.addEventListener('click', function (event) {
        event.preventDefault();
        clicks += 1;
        clearTimeout(resetTimer);

        if (clicks >= 5) {
            clicks = 0;
            openQuiz();
            return;
        }

        resetTimer = setTimeout(function () {
            clicks = 0;
            window.location.href = brand.getAttribute('href') || '/';
        }, 550);
    });

    function escapeHtml(str) {
        var div = document.createElement('div');
        div.textContent = str == null ? '' : str;
        return div.innerHTML;
    }

    function shuffled(arr) {
        var copy = arr.slice();
        for (var i = copy.length - 1; i > 0; i--) {
            var j = Math.floor(Math.random() * (i + 1));
            var tmp = copy[i];
            copy[i] = copy[j];
            copy[j] = tmp;
        }
        return copy;
    }

    function onKeydown(event) {
        if (event.key === 'Escape') closeQuiz();
    }

    function closeQuiz() {
        if (!overlay) return;
        document.removeEventListener('keydown', onKeydown);
        document.body.classList.remove('egg-open');
        overlay.remove();
        overlay = null;
    }

    function renderIntro() {
        overlay.querySelector('.egg3-card').innerHTML =
            '<div class="egg3-title">GTA oder Harburg?</div>' +
            '<p class="egg3-text">' +
            'Du bekommst ' + ROUND_SIZE + ' kurz umschriebene Vorfälle. Manche sind echte Polizeimeldungen aus ' +
            'Harburg, andere sind Missionen aus GTA: San Andreas. Errätst du, was echt ist?' +
            '</p>' +
            '<button type="button" class="egg3-btn egg3-btn-start">Los geht\'s</button>';
        overlay.querySelector('.egg3-btn-start').addEventListener('click', startRound);
    }

    function startRound() {
        round = shuffled(allCases).slice(0, ROUND_SIZE);
        index = 0;
        score = 0;
        renderQuestion();
    }

    function renderQuestion() {
        var current = round[index];
        var card = overlay.querySelector('.egg3-card');
        card.innerHTML =
            '<div class="egg3-progress">Frage ' + (index + 1) + ' / ' + round.length + '</div>' +
            '<p class="egg3-clue">„' + escapeHtml(current.clue) + '“</p>' +
            '<div class="egg3-choices">' +
            '<button type="button" class="egg3-btn egg3-choice" data-answer="Harburg">📍 Harburg</button>' +
            '<button type="button" class="egg3-btn egg3-choice" data-answer="GTA">🎮 GTA</button>' +
            '</div>' +
            '<div class="egg3-reveal"></div>';
        card.scrollTop = 0;

        card.querySelectorAll('.egg3-choice').forEach(function (btn) {
            btn.addEventListener('click', function () {
                answer(current, btn.getAttribute('data-answer'), card);
            });
        });
    }

    function answer(current, picked, card) {
        var correct = picked === current.answer;
        if (correct) score += 1;

        card.querySelectorAll('.egg3-choice').forEach(function (btn) {
            btn.disabled = true;
            var btnAnswer = btn.getAttribute('data-answer');
            if (btnAnswer === current.answer) btn.classList.add('egg3-correct');
            else if (btnAnswer === picked) btn.classList.add('egg3-wrong');
        });

        var linkHtml = current.linkUrl
            ? '<a class="egg3-source" href="' + current.linkUrl + '" target="_blank" rel="noopener">Quelle: ' +
              escapeHtml(current.linkLabel) + '</a>'
            : '';

        card.querySelector('.egg3-reveal').innerHTML =
            '<p class="egg3-verdict ' + (correct ? 'egg3-verdict-ok' : 'egg3-verdict-bad') + '">' +
            (correct ? 'Richtig! ' : 'Leider falsch! ') + 'Das war ' + escapeHtml(current.answer) + '.' +
            '</p>' +
            linkHtml +
            '<button type="button" class="egg3-btn egg3-btn-next">' +
            (index + 1 < round.length ? 'Weiter' : 'Ergebnis anzeigen') +
            '</button>';

        // The reveal (verdict + source link + button) can push past the card's visible
        // height on a phone screen - scroll it into view so "Weiter" is never hidden
        // below the fold without an obvious way to find it.
        requestAnimationFrame(function () {
            card.scrollTop = card.scrollHeight;
        });

        card.querySelector('.egg3-btn-next').addEventListener('click', function () {
            index += 1;
            if (index < round.length) renderQuestion();
            else renderResult();
        });
    }

    function resultFlavor() {
        if (score === round.length) return 'Perfekt! Du kennst Harburg (oder Los Santos) in- und auswendig. 🏆';
        if (score >= round.length - 1) return 'Sehr stark erraten! 🔥';
        if (score >= round.length / 2) return 'Solide - die Realität ist manchmal verrückter als GTA.';
        return 'Harburg ist offenbar wilder, als du dachtest. 😅';
    }

    function renderResult() {
        overlay.querySelector('.egg3-card').innerHTML =
            '<div class="egg3-title">' + score + ' / ' + round.length + '</div>' +
            '<p class="egg3-text">' + resultFlavor() + '</p>' +
            '<button type="button" class="egg3-btn egg3-btn-start">Nochmal spielen</button>';
        overlay.querySelector('.egg3-btn-start').addEventListener('click', startRound);
    }

    function openQuiz() {
        if (overlay) return;

        overlay = document.createElement('div');
        overlay.className = 'egg3-overlay';
        overlay.innerHTML =
            '<div class="egg3-scrim"></div>' +
            '<button type="button" class="egg3-close" aria-label="Schließen">&times;</button>' +
            '<div class="egg3-card">' +
            '<div class="egg3-title">GTA oder Harburg?</div>' +
            '<p class="egg3-text">Lade Fälle…</p>' +
            '</div>';
        document.body.appendChild(overlay);
        document.body.classList.add('egg-open');

        overlay.querySelector('.egg3-close').addEventListener('click', closeQuiz);
        overlay.querySelector('.egg3-scrim').addEventListener('click', closeQuiz);
        document.addEventListener('keydown', onKeydown);

        if (allCases) {
            renderIntro();
            return;
        }

        fetch(DATA_URL)
            .then(function (res) { return res.json(); })
            .then(function (data) {
                // Gag: for now every case really happened in Harburg - "GTA" is never
                // the right answer, so the "twist" is just how wild Harburg actually is.
                // Remove this filter to bring the real GTA cases back into the pool.
                allCases = data.filter(function (c) { return c.answer === 'Harburg'; });
                if (overlay) renderIntro();
            })
            .catch(function () {
                if (overlay) overlay.querySelector('.egg3-card .egg3-text').textContent =
                    'Die Fälle konnten nicht geladen werden.';
            });
    }
});
