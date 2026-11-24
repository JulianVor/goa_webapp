/* Hidden "GRUMBRECHTSTRASSE ODER AUSGEDACHT?" mystery quiz: 7x schneller Klick auf
   das große Hero-Logo auf der Hauptseite ("/") startet eine kleine, in sich
   geschlossene Mystery-Show im Stil alter TV-Rätselsendungen. 4 Episoden a 5
   Geschichten, jede Episode mit eigener Eröffnung und eigener, in sich
   abgeschlossener Auflösung - keine Episode darf etwas über eine andere verraten. */
document.addEventListener('DOMContentLoaded', function () {
    if (window.location.pathname !== '/') return;

    var heroLogo = document.querySelector('.hero-logo') || document.querySelector('.hero-title');
    if (!heroLogo) return;

    var DATA_URL = '/mystery/grumbrechtstrasse-episoden.json';
    var STORAGE_KEY = 'grumbrechtstrasse-oder-ausgedacht-progress';
    var FONT_HREF = 'https://fonts.googleapis.com/css2?family=Cinzel:wght@400;600;700&display=swap';

    var clicks = 0;
    var resetTimer = null;
    var overlay = null;
    var data = null;
    var reduceMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    // Counts by click position rather than event.target/a listener on heroLogo itself:
    // the GTA quiz (easter-egg-gta.js) already opens a full-screen overlay on the 5th
    // click of this same element, which would otherwise swallow clicks 6 and 7 before
    // they ever reach heroLogo. A capture-phase listener on document always sees every
    // click regardless of what overlay is on top, so counting still reaches 7.
    document.addEventListener('click', function (event) {
        var rect = heroLogo.getBoundingClientRect();
        if (event.clientX < rect.left || event.clientX > rect.right
            || event.clientY < rect.top || event.clientY > rect.bottom) {
            return;
        }

        clicks += 1;
        clearTimeout(resetTimer);

        if (clicks >= 7) {
            clicks = 0;
            openShow();
            return;
        }

        resetTimer = setTimeout(function () {
            clicks = 0;
        }, 650);
    }, true);

    function escapeHtml(str) {
        var div = document.createElement('div');
        div.textContent = str == null ? '' : str;
        return div.innerHTML;
    }

    function ensureFont() {
        if (document.querySelector('link[data-myst-font]')) return;
        var link = document.createElement('link');
        link.rel = 'stylesheet';
        link.href = FONT_HREF;
        link.setAttribute('data-myst-font', '1');
        document.head.appendChild(link);
    }

    // ---- progress persistence -------------------------------------------------

    function loadProgress() {
        try {
            var raw = JSON.parse(window.localStorage.getItem(STORAGE_KEY));
            return raw && typeof raw === 'object' ? raw : {};
        } catch (e) {
            return {};
        }
    }

    function saveProgress(progress) {
        try {
            window.localStorage.setItem(STORAGE_KEY, JSON.stringify(progress));
        } catch (e) {
            // private browsing / blocked storage - the show still works, just without memory
        }
    }

    function episodeProgress(progress, episodeId) {
        return progress[episodeId] || { answers: {}, completedAt: null };
    }

    function episodeStatus(ep, progress) {
        var p = episodeProgress(progress, ep.id);
        var answered = Object.keys(p.answers).length;
        if (p.completedAt) return 'completed';
        if (answered > 0) return 'started';
        return 'new';
    }

    function episodeScore(ep, p) {
        var score = 0;
        ep.stories.forEach(function (s) {
            if (p.answers[s.id] === s.correct_answer) score += 1;
        });
        return score;
    }

    // ---- show state -------------------------------------------------------

    var current = null; // { episode, storyIndex, progress }

    function onKeydown(event) {
        if (event.key === 'Escape') closeShow();
    }

    function closeShow() {
        if (!overlay) return;
        document.removeEventListener('keydown', onKeydown);
        document.body.classList.remove('egg-open');
        overlay.remove();
        overlay = null;
        current = null;
    }

    function stage() {
        return overlay.querySelector('.myst-stage');
    }

    function setStage(html) {
        var el = stage();
        el.innerHTML = html;
        el.scrollTop = 0;
        if (!reduceMotion) {
            el.classList.remove('myst-pop');
            void el.offsetWidth;
            el.classList.add('myst-pop');
        }
    }

    // ---- episode select -----------------------------------------------------

    function renderEpisodeSelect() {
        var progress = loadProgress();
        var cards = data.episodes.map(function (ep) {
            var status = episodeStatus(ep, progress);
            var p = episodeProgress(progress, ep.id);
            var statusLabel = status === 'completed' ? 'ABGESCHLOSSEN' : (status === 'started' ? 'BEGONNEN' : 'NOCH NICHT GESPIELT');
            var scoreLine = status === 'completed'
                ? '<div class="myst-card-score">' + episodeScore(ep, p) + ' / ' + ep.stories.length + ' richtig erkannt</div>'
                : '';
            var actions = status === 'completed'
                ? '<div class="myst-card-actions">' +
                  '<button type="button" class="myst-btn myst-btn-small" data-action="results" data-episode="' + ep.id + '">Ergebnis ansehen</button>' +
                  '<button type="button" class="myst-btn myst-btn-small myst-btn-ghost" data-action="restart" data-episode="' + ep.id + '">Erneut spielen</button>' +
                  '</div>'
                : '<button type="button" class="myst-btn myst-btn-small" data-action="play" data-episode="' + ep.id + '">' +
                  (status === 'started' ? 'Fortsetzen' : 'Episode starten') + '</button>';
            return '<div class="myst-ep-card myst-status-' + status + '">' +
                '<div class="myst-ep-number">EPISODE ' + ep.number + '</div>' +
                '<div class="myst-ep-title">' + escapeHtml(ep.title) + '</div>' +
                '<div class="myst-ep-subtitle">' + escapeHtml(ep.subtitle) + '</div>' +
                '<div class="myst-ep-status">' + statusLabel + '</div>' +
                scoreLine +
                actions +
                '</div>';
        }).join('');

        setStage(
            '<div class="myst-select">' +
            '<div class="myst-logo-title">' + escapeHtml(data.project.title) + '</div>' +
            '<div class="myst-logo-subtitle">' + escapeHtml(data.project.subtitle) + '</div>' +
            '<p class="myst-tagline">Fünf Geschichten. Zwei Möglichkeiten. Eine Entscheidung.</p>' +
            '<div class="myst-ep-grid">' + cards + '</div>' +
            '</div>'
        );

        stage().querySelectorAll('[data-action]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var episodeId = btn.getAttribute('data-episode');
                var ep = data.episodes.filter(function (e) { return e.id === episodeId; })[0];
                var action = btn.getAttribute('data-action');
                if (action === 'results') openResults(ep);
                else if (action === 'restart') startEpisode(ep, true);
                else startEpisode(ep, false);
            });
        });
    }

    // ---- episode intro ------------------------------------------------------

    function startEpisode(ep, forceRestart) {
        var progress = loadProgress();
        if (forceRestart) {
            delete progress[ep.id];
            saveProgress(progress);
        }
        var p = episodeProgress(progress, ep.id);
        var answeredCount = Object.keys(p.answers).length;

        current = { episode: ep, storyIndex: answeredCount };

        if (answeredCount === 0) {
            renderIntro(ep);
        } else if (answeredCount < ep.stories.length) {
            renderStory();
        } else {
            runReveal(ep, 0);
        }
    }

    function renderIntro(ep) {
        setStage(
            '<div class="myst-intro">' +
            '<div class="myst-ep-number">EPISODE ' + ep.number + '</div>' +
            '<div class="myst-ep-title myst-ep-title-big">' + escapeHtml(ep.title) + '</div>' +
            '<div class="myst-ep-subtitle">' + escapeHtml(ep.subtitle) + '</div>' +
            '<p class="myst-narration">' + escapeHtml(ep.intro) + '</p>' +
            '<button type="button" class="myst-btn" data-next>Episode starten</button>' +
            '</div>'
        );
        stage().querySelector('[data-next]').addEventListener('click', renderStory);
    }

    // ---- story playback -------------------------------------------------------

    function renderStory() {
        var ep = current.episode;
        var story = ep.stories[current.storyIndex];

        setStage(
            '<div class="myst-story">' +
            '<div class="myst-progress">GESCHICHTE ' + (current.storyIndex + 1) + ' VON ' + ep.stories.length + '</div>' +
            '<div class="myst-story-title">' + escapeHtml(story.title) + '</div>' +
            '<p class="myst-narration">' + escapeHtml(story.narration) + '</p>' +
            '<button type="button" class="myst-btn" data-next>Weiter</button>' +
            '</div>'
        );
        stage().querySelector('[data-next]').addEventListener('click', function () {
            renderDecision(story);
        });
    }

    function renderDecision(story) {
        var ep = current.episode;
        setStage(
            '<div class="myst-story">' +
            '<div class="myst-progress">GESCHICHTE ' + (current.storyIndex + 1) + ' VON ' + ep.stories.length + '</div>' +
            '<p class="myst-decision-prompt">' + escapeHtml(story.decision_prompt) + '</p>' +
            '<div class="myst-choices">' +
            '<button type="button" class="myst-choice" data-answer="grumbrechtstrasse">GRUMBRECHTSTRASSE</button>' +
            '<button type="button" class="myst-choice" data-answer="ausgedacht">AUSGEDACHT</button>' +
            '</div>' +
            '</div>'
        );
        stage().querySelectorAll('.myst-choice').forEach(function (btn) {
            btn.addEventListener('click', function () {
                answerStory(story, btn.getAttribute('data-answer'));
            });
        });
    }

    function answerStory(story, choice) {
        var ep = current.episode;
        var progress = loadProgress();
        var p = episodeProgress(progress, ep.id);
        p.answers[story.id] = choice;
        progress[ep.id] = p;
        saveProgress(progress);

        setStage('<div class="myst-saved">IHRE ENTSCHEIDUNG WURDE GESPEICHERT.</div>');

        setTimeout(function () {
            if (!overlay) return;
            current.storyIndex += 1;
            if (current.storyIndex < ep.stories.length) {
                renderStory();
            } else {
                runReveal(ep, 0);
            }
        }, reduceMotion ? 300 : 1100);
    }

    // ---- reveal sequence ------------------------------------------------------

    function runReveal(ep, stepIndex) {
        var steps = ep.reveal_steps;
        if (stepIndex >= steps.length) {
            renderRevealBreakdown(ep);
            return;
        }
        var step = steps[stepIndex];

        if (step.type === 'dramatic_pause') {
            setStage('<div class="myst-pause"></div>');
            var delay = reduceMotion ? 200 : (step.duration_ms || 1200);
            setTimeout(function () {
                if (overlay) runReveal(ep, stepIndex + 1);
            }, delay);
            return;
        }

        var cls = step.type === 'title_card' ? 'myst-title-card' : 'myst-monologue';
        setStage(
            '<div class="myst-reveal">' +
            '<div class="' + cls + '">' + escapeHtml(step.text) + '</div>' +
            '<button type="button" class="myst-btn" data-next>Weiter</button>' +
            '</div>'
        );
        stage().querySelector('[data-next]').addEventListener('click', function () {
            runReveal(ep, stepIndex + 1);
        });
    }

    function renderRevealBreakdown(ep) {
        var progress = loadProgress();
        var p = episodeProgress(progress, ep.id);

        var rows = ep.stories.map(function (s) {
            var picked = p.answers[s.id];
            var pickedLabel = picked === 'ausgedacht' ? 'AUSGEDACHT' : 'GRUMBRECHTSTRASSE';
            var correct = picked === s.correct_answer;
            return '<div class="myst-reveal-row">' +
                '<div class="myst-reveal-row-title">' + escapeHtml(s.title) + '</div>' +
                '<div class="myst-reveal-row-answer">Ihre Antwort: ' + pickedLabel +
                (correct ? ' <span class="myst-correct">✓</span>' : ' <span class="myst-wrong">✕</span>') + '</div>' +
                '<div class="myst-reveal-row-truth">' + escapeHtml(ep.story_result_label) + '</div>' +
                '<p class="myst-reveal-row-explanation">' + escapeHtml(s.answer_explanation) + '</p>' +
                '</div>';
        }).join('');

        setStage(
            '<div class="myst-reveal-breakdown">' +
            rows +
            '<button type="button" class="myst-btn" data-next>Zur Auswertung</button>' +
            '</div>'
        );
        stage().querySelector('[data-next]').addEventListener('click', function () {
            if (!p.completedAt) {
                p.completedAt = Date.now();
                progress[ep.id] = p;
                saveProgress(progress);
            }
            openResults(ep);
        });
    }

    // ---- results ---------------------------------------------------------------

    function openResults(ep) {
        var progress = loadProgress();
        var p = episodeProgress(progress, ep.id);
        var score = episodeScore(ep, p);
        var grumbrechtCount = 0, ausgedachtCount = 0;
        ep.stories.forEach(function (s) {
            if (p.answers[s.id] === 'ausgedacht') ausgedachtCount += 1;
            else grumbrechtCount += 1;
        });
        var comment = (data.ui.results_comments_by_score || {})[String(score)] || '';

        setStage(
            '<div class="myst-results">' +
            '<div class="myst-ep-number">EPISODE ' + ep.number + ' ABGESCHLOSSEN</div>' +
            '<div class="myst-results-score">' + score + ' VON ' + ep.stories.length + ' RICHTIG ERKANNT</div>' +
            '<p class="myst-results-stats">' + ep.stories.length + ' Geschichten gehört · ' +
            grumbrechtCount + '× Grumbrechtstraße gewählt · ' + ausgedachtCount + '× Ausgedacht gewählt</p>' +
            '<p class="myst-results-comment">' + escapeHtml(comment) + '</p>' +
            '<div class="myst-card-actions myst-results-actions">' +
            '<button type="button" class="myst-btn" data-action="select">Zur Episodenauswahl</button>' +
            '<button type="button" class="myst-btn myst-btn-ghost" data-action="replay">Episode erneut spielen</button>' +
            '</div>' +
            '</div>'
        );
        stage().querySelector('[data-action="select"]').addEventListener('click', renderEpisodeSelect);
        stage().querySelector('[data-action="replay"]').addEventListener('click', function () {
            startEpisode(ep, true);
        });
    }

    // ---- bootstrapping -------------------------------------------------------

    function openShow() {
        if (overlay) return;
        ensureFont();

        // Reaching 7 clicks always passes through 5 first, which opens the GTA quiz
        // (easter-egg-gta.js, class egg3-overlay) - close it so it doesn't linger
        // stacked underneath, half-finished, once this overlay is closed again.
        document.querySelectorAll('.egg-overlay, .egg2-overlay, .egg3-overlay').forEach(function (el) {
            el.remove();
        });
        document.body.classList.remove('egg-open');

        overlay = document.createElement('div');
        overlay.className = 'myst-overlay';
        overlay.innerHTML =
            '<div class="myst-grain"></div>' +
            '<div class="myst-vignette"></div>' +
            '<button type="button" class="myst-close" aria-label="Schließen">&times;</button>' +
            '<div class="myst-frame"><div class="myst-stage">Lade Archiv…</div></div>';
        document.body.appendChild(overlay);
        document.body.classList.add('egg-open');

        overlay.querySelector('.myst-close').addEventListener('click', closeShow);
        document.addEventListener('keydown', onKeydown);

        if (data) {
            renderEpisodeSelect();
            return;
        }

        fetch(DATA_URL)
            .then(function (res) { return res.json(); })
            .then(function (json) {
                data = json;
                if (overlay) renderEpisodeSelect();
            })
            .catch(function () {
                if (overlay) setStage('<p class="myst-narration">Das Archiv konnte nicht geladen werden.</p>');
            });
    }
});
