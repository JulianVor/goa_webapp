/* The home-page logo dispatches goa:open-mystery on its seventh tap. */
document.addEventListener('DOMContentLoaded', function () {
    if (window.location.pathname !== '/') return;
    const logo = document.querySelector('.hero-logo, .hero-title');
    if (!logo) return;

    const storageKey = 'grumbrechtstrasse-oder-ausgedacht-progress';
    const labels = { grumbrechtstrasse: 'Grumbrechtstraße', ausgedacht: 'Ausgedacht' };
    const synth = window.speechSynthesis;
    let archive, dialog, stage, episode, storyIndex, opener;
    let sessionProgress = { version: 2, episodes: {} };
    let storageAvailable = true;
    let readAloud = false;
    let spokenText = '';
    let transitionTimer;
    let music;
    let musicEnabled = false;
    let silentScene = false;
    let reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    logo.addEventListener('goa:open-mystery', openShow);
    const keyboardEntry = document.createElement('button');
    keyboardEntry.type = 'button';
    keyboardEntry.className = 'myst-key-entry';
    keyboardEntry.textContent = 'Grumbrechtstraße oder ausgedacht? öffnen';
    keyboardEntry.addEventListener('click', openShow);
    logo.after(keyboardEntry);

    function escapeHtml(text) {
        const el = document.createElement('span');
        el.textContent = text;
        return el.innerHTML;
    }

    // The first edition used the same story IDs in different episodes. Look up
    // unchanged questions by ID; an old result never completes a new finale.
    function migrate(legacy) {
        const revised = new Set(['story-02', 'story-15', 'story-18', 'story-21']);
        const oldAnswers = Object.assign({}, ...Object.values(legacy).map(p => p?.answers));
        const episodes = {};
        for (const ep of archive.episodes) {
            const answers = {};
            for (const story of ep.stories) {
                const choice = oldAnswers[story.id];
                if (!revised.has(story.id) && Object.hasOwn(labels, choice)) answers[story.id] = choice;
            }
            if (Object.keys(answers).length) episodes[ep.id] = { answers };
        }
        return { version: 2, episodes, migrationNotice: true };
    }

    function persist() {
        if (!storageAvailable) return;
        try { localStorage.setItem(storageKey, JSON.stringify(sessionProgress)); }
        catch { storageAvailable = false; }
    }

    function readProgress() {
        if (!storageAvailable) return sessionProgress;
        try {
            const raw = JSON.parse(localStorage.getItem(storageKey));
            if (raw?.version === 2 && raw.episodes) sessionProgress = raw;
            else if (raw && archive && Object.keys(raw).some(key => key.startsWith('episode-'))) {
                sessionProgress = migrate(raw);
                persist();
            }
        } catch { /* Keep the current record if storage is unavailable or unreadable. */ }
        return sessionProgress;
    }

    function record(ep = episode) {
        const saved = readProgress().episodes[ep.id];
        return saved && saved.answers && typeof saved.answers === 'object'
            ? saved : { answers: {}, completedAt: null };
    }

    function save(p) {
        sessionProgress.episodes[episode.id] = p;
        persist();
    }

    function checkpoint(phase, index = 0) {
        const p = record();
        p.phase = phase;
        p.index = index;
        save(p);
    }

    function unlocked(ep) {
        return archive.episodes.slice(0, archive.episodes.indexOf(ep)).every(previous => {
            const answers = record(previous).finishedAnswers;
            return answers && firstUnanswered(previous, { answers }) === -1;
        });
    }

    function firstUnanswered(ep, p) {
        return ep.stories.findIndex(story => !Object.hasOwn(labels, p.answers[story.id]));
    }

    function score(ep, p) {
        return ep.stories.filter(story => p.answers[story.id] === story.correct_answer).length;
    }

    function number(value) {
        return String(value).padStart(2, '0');
    }

    function button(label, action, secondary = false) {
        return `<button type="button" class="myst-btn${secondary ? ' myst-secondary' : ''}" data-action="${action}">${label}</button>`;
    }

    function speak() {
        if (!readAloud || !synth || !spokenText) return;
        const line = new SpeechSynthesisUtterance(spokenText);
        line.lang = 'de-DE';
        line.rate = 0.94;
        synth.speak(line);
    }

    function updateMusic() {
        if (!dialog?.open || !musicEnabled || silentScene || document.hidden) {
            music?.pause();
            return;
        }
        if (!music) {
            music = new Audio('/mystery/nachtprogramm.mp3');
            music.loop = true;
            music.volume = .45;
        }
        if (music.paused) music.play().catch(() => {});
    }

    function setStage(html, narration = '', silent = false) {
        clearTimeout(transitionTimer);
        silentScene = silent;
        updateMusic();
        if (readAloud) synth.cancel();
        spokenText = narration;
        stage.innerHTML = html;
        stage.scrollTop = 0;
        stage.querySelector('h1, h2')?.focus({ preventScroll: true });
        dialog.querySelector('[data-action="archive"]').hidden = !episode;
        speak();
    }

    function renderArchive() {
        episode = null;
        const entries = archive.episodes.map((ep, index) => {
            const p = record(ep);
            const next = firstUnanswered(ep, p);
            let status = `${ep.stories.length} Geschichten`;
            let action = 'Folge starten';
            const locked = !unlocked(ep);
            if (locked) {
                status = `Nach Abschluss von Folge ${number(ep.number - 1)}`;
                action = 'Gesperrt';
            } else if (p.finishedAnswers) {
                status = `${score(ep, { answers: p.finishedAnswers })} von ${ep.stories.length} richtig`;
                action = 'Bilanz ansehen';
                if (p.phase !== 'results' && p.phase !== 'series') action = 'Wiederholung fortsetzen';
            } else if (p.phase || Object.keys(p.answers).length > 0) {
                status = next === -1 ? 'Im Finale' : `Bei Fall ${next + 1} von ${ep.stories.length}`;
                action = 'Fortsetzen';
            }
            return `<li><button type="button" class="myst-episode" data-episode="${index}" ${locked ? 'disabled' : ''}>
                <span class="myst-thumb myst-art-${ep.number}" aria-hidden="true"></span>
                <span class="myst-episode-copy"><span class="myst-kicker">Folge ${number(ep.number)}</span>
                <strong>${escapeHtml(ep.title)}</strong><span class="myst-meta">${status}</span>
                <span class="myst-link">${action}</span></span></button></li>`;
        }).join('');
        const title = escapeHtml(archive.project.title).replace(/ (oder .+)$/, '<br><em>$1</em>');
        setStage(`<div class="myst-archive">
            <div class="myst-opening"><div class="myst-opening-copy">
                <p class="myst-kicker">Mystery aus Hamburg</p>
                <h1 tabindex="-1">${title}</h1>
                <p>${escapeHtml(archive.project.subtitle)}</p>
            </div></div>
            <section class="myst-program"><div class="myst-program-heading"><h2>Folgenübersicht</h2>
            <p>Ihr Tipp nach jedem Fall. Die Auflösung am Ende der Folge.</p></div>
            ${readProgress().migrationNotice ? '<div class="myst-notice"><p>Neue Serienfassung: Tipps zu unveränderten Fällen wurden übernommen. Überarbeitete Fälle spielen Sie neu. Die Freischaltung richtet sich nach den neuen Folgenauflösungen.</p>' + button('Verstanden', 'dismiss', true) + '</div>' : ''}
            <ul class="myst-episodes">${entries}</ul></section></div>`);
    }

    function cutaway({ studio = false, caption, kicker, title, copy, controls, narration = '' }) {
        const art = studio ? 'myst-studio' : `myst-art-${episode.number}`;
        setStage(`<section class="myst-scene">
            <div class="myst-still ${art}"><span>${studio ? 'Jonathan Frakes · KI-Inszenierung' : escapeHtml(caption) + ' · Szenenbild'}</span></div>
            <div class="myst-script"><p class="myst-kicker">Folge ${number(episode.number)} · ${escapeHtml(kicker)}</p>
            <h2 tabindex="-1">${escapeHtml(title)}</h2>${copy}
            <div class="myst-actions">${controls}</div></div></section>`, narration);
    }

    function openEpisode(ep, restart = false) {
        if (!ep || !unlocked(ep)) return;
        episode = ep;
        if (restart) {
            save({ answers: {}, finishedAnswers: record().finishedAnswers });
        }
        const p = record();
        storyIndex = firstUnanswered(ep, p);
        if (p.finishedAnswers && p.phase === 'series') return renderSeriesEnd();
        if (p.finishedAnswers && p.phase === 'results') return renderResults();
        if (p.phase && storyIndex === -1) {
            if (p.phase === 'verdict') return renderVerdict(p.index);
            if (p.phase === 'finale' && ep.number === 4) return renderFinale(p.index);
            return renderFinaleIntro();
        }
        if (p.phase === 'decision') return renderDecision();
        if (p.phase === 'story') return renderStory();
        cutaway({
            studio: true, caption: 'Im Studio', kicker: 'Anmoderation', title: ep.title,
            copy: `<p class="myst-lede">${escapeHtml(ep.subtitle)}</p><p>${escapeHtml(ep.intro)}</p>`,
            controls: button(storyIndex > 0 ? 'Zum nächsten offenen Fall' : storyIndex === -1 ? 'Zur Auflösung' : 'Zum ersten Fall', 'story'),
            narration: ep.intro
        });
    }

    function caseLabel(index) {
        return `Fall ${number(index + 1)} von ${number(episode.stories.length)}`;
    }

    function renderStory() {
        if (storyIndex === -1) return renderFinaleIntro();
        checkpoint('story');
        const story = episode.stories[storyIndex];
        cutaway({
            caption: episode.title, kicker: caseLabel(storyIndex), title: story.title,
            copy: `<p class="myst-lede">${escapeHtml(story.teaser)}</p><p>${escapeHtml(story.narration)}</p>`, controls: button('Mein Urteil abgeben', 'decision'),
            narration: `${story.title}. ${story.narration}`
        });
    }

    function renderDecision() {
        checkpoint('decision');
        const story = episode.stories[storyIndex];
        cutaway({
            studio: true, caption: 'Zurück im Studio', kicker: caseLabel(storyIndex), title: 'Ihr Urteil?',
            copy: `<p class="myst-lede">${escapeHtml(story.decision_prompt)}</p>`,
            controls: button(labels.grumbrechtstrasse, 'grumbrechtstrasse') + button(labels.ausgedacht, 'ausgedacht', true)
                + '<button type="button" class="myst-reread" data-action="story">Fall noch einmal lesen</button>',
            narration: story.decision_prompt
        });
    }

    function answer(choice) {
        const p = record();
        const story = episode.stories[storyIndex];
        if (!story || p.phase !== 'decision') return;
        if (!Object.hasOwn(labels, p.answers[story.id])) p.answers[story.id] = choice;
        p.phase = 'story';
        save(p);
        storyIndex = firstUnanswered(episode, p);
        setStage('<div class="myst-intertitle"><h2 tabindex="-1">IHRE ENTSCHEIDUNG WURDE GESPEICHERT.</h2></div>');
        transitionTimer = setTimeout(renderStory, reducedMotion ? 750 : 1350);
    }

    function renderFinaleIntro() {
        if (firstUnanswered(episode, record()) !== -1) return;
        checkpoint('closing');
        cutaway({
            studio: true, caption: 'Zurück im Studio', kicker: 'Das Finale', title: 'Was ist wirklich geschehen?',
            copy: `<p>${escapeHtml(episode.reveal_intro)}</p>`,
            controls: episode.number === 4 ? button('Weiter', 'finale') : button('Den ersten Fall auflösen', 'verdict'),
            narration: episode.reveal_intro
        });
    }

    // The final broadcast intercuts two studio passages with blackouts and
    // individual title cards. Store the cut so a reload resumes the same shot.
    function renderFinale(index) {
        if (episode.number !== 4 || firstUnanswered(episode, record()) !== -1 || !unlocked(episode)) return;
        const finale = archive.series_finale;
        const shots = [
            { pause: 1200 },
            { speech: finale.recap, heading: 'Erinnern Sie sich?' },
            { pause: 2600 },
            ...finale.cards.map(title => ({ title, duration: 1900 })),
            { speech: finale.signoff, heading: 'Ein einziges Haus' },
            { title: finale.title }
        ];
        const shot = shots[index];
        if (!shot) return renderVerdict(0);
        checkpoint('finale', index);
        const next = button(index === shots.length - 1 ? 'Den ersten Fall auflösen' : 'Weiter', 'next-shot');
        const controls = next + (index < shots.length - 1 ? button('Inszenierung überspringen', 'skip-finale', true) : '');
        if (shot.speech) {
            cutaway({ studio: true, kicker: 'Serienfinale', title: shot.heading, copy: `<p>${escapeHtml(shot.speech)}</p>`, controls, narration: shot.speech });
        } else {
            const heading = shot.title || 'Schwarzblende';
            setStage(`<div class="myst-intertitle myst-final-card"><h2 tabindex="-1" ${shot.pause ? 'class="myst-visually-hidden"' : ''}>${escapeHtml(heading)}</h2>
                <div class="myst-actions">${controls}</div></div>`, shot.pause ? '' : heading, Boolean(shot.pause));
            const delay = shot.pause || shot.duration;
            if (delay && (!reducedMotion || shot.pause)) {
                transitionTimer = setTimeout(() => renderFinale(index + 1), reducedMotion ? 0 : delay);
            }
        }
    }

    function renderVerdict(index) {
        const p = record();
        if (firstUnanswered(episode, p) !== -1) return;
        const stories = episode.stories;
        storyIndex = Math.max(0, Math.min(index, stories.length - 1));
        const story = stories[storyIndex];
        checkpoint('verdict', storyIndex);
        const right = p.answers[story.id] === story.correct_answer;
        const verdict = story.correct_answer === 'grumbrechtstrasse' ? 'TATSÄCHLICH GESCHEHEN' : 'FREI ERFUNDEN';
        cutaway({
            caption: story.title, kicker: `Auflösung · ${caseLabel(storyIndex)}`, title: verdict,
            copy: `<p class="myst-case-name">${escapeHtml(story.title)}</p><p>${escapeHtml(story.answer_explanation)}</p>
                <p class="myst-meta">Laut persönlichem Erfahrungsbericht des ehemaligen Bewohners. Eine unabhängige Überprüfung liegt nicht vor.</p>
                <p class="myst-pick"><strong>${right ? 'Richtig getippt' : 'Danebengetippt'}</strong> · Ihr Tipp war „${labels[p.answers[story.id]]}“.</p>`,
            controls: storyIndex + 1 < stories.length ? button('Nächsten Fall auflösen', 'next-verdict') : button('Zur Bilanz', 'finish'),
            narration: `${story.title}. ${verdict} ${story.answer_explanation}`
        });
    }

    function renderResults() {
        const p = record();
        const answers = p.finishedAnswers;
        if (!answers) return;
        const hits = score(episode, { answers });
        const total = episode.stories.length;
        const comment = archive.ui.results_comments_by_score[hits] || '';
        const rows = episode.stories.map(s => `<li><strong>${escapeHtml(s.title)}</strong>
            <span>Ihr Tipp: ${labels[answers[s.id]]} · Auflösung: ${labels[s.correct_answer]}</span></li>`).join('');
        const next = episode.number < archive.episodes.length
            ? button('Weiter zu Folge ' + number(episode.number + 1), 'next-episode')
            : button('Zur Serienbilanz', 'series');
        cutaway({
            studio: true, caption: 'Abspann', kicker: 'Ihre Bilanz', title: `${hits} von ${total} richtig`,
            copy: `<ol class="myst-breakdown">${rows}</ol><p class="myst-signoff">${escapeHtml(comment)}</p>`,
            controls: next + button('Erneut spielen', 'restart', true),
            narration: `${hits} von ${total} richtig. ${comment}`
        });
    }

    function renderSeriesEnd() {
        if (episode.number !== 4 || !unlocked(episode) || !record().finishedAnswers) return;
        checkpoint('series');
        const scores = archive.episodes.map(ep => score(ep, { answers: record(ep).finishedAnswers }));
        const total = scores.reduce((sum, value) => sum + value, 0);
        const rows = archive.episodes.map((ep, i) => `<li><strong>${number(ep.number)} · ${escapeHtml(ep.title)}</strong>
            <span>${scores[i]} von ${ep.stories.length} richtig</span></li>`).join('');
        cutaway({ studio: true, kicker: 'Serienabschluss', title: `${total} von 20 richtig`,
            copy: `<p class="myst-lede">${escapeHtml(archive.series_finale.title)}</p><ol class="myst-breakdown">${rows}</ol>
                <p class="myst-meta">${escapeHtml(archive.series_finale.source_note)}</p>`,
            controls: button('Zur Folgenübersicht', 'archive') + button('Folgenbilanz ansehen', 'results', true),
            narration: `${total} von 20 richtig. ${archive.series_finale.source_note}` });
    }

    function onAction(event) {
        const target = event.target.closest('button');
        if (!target) return;
        dialog.querySelector('details').open = false;
        if (target.hasAttribute('data-episode')) return openEpisode(archive.episodes[Number(target.dataset.episode)]);
        switch (target.dataset.action) {
            case 'close': dialog.close(); break;
            case 'archive': renderArchive(); break;
            case 'reload': loadArchive(); break;
            case 'dismiss':
                delete readProgress().migrationNotice;
                persist();
                renderArchive();
                break;
            case 'story': renderStory(); break;
            case 'decision': renderDecision(); break;
            case 'grumbrechtstrasse':
            case 'ausgedacht': answer(target.dataset.action); break;
            case 'verdict': renderVerdict(0); break;
            case 'finale': renderFinale(0); break;
            case 'next-shot': renderFinale(record().index + 1); break;
            case 'skip-finale': renderVerdict(0); break;
            case 'next-verdict': renderVerdict(storyIndex + 1); break;
            case 'finish': {
                const p = record();
                if (p.phase !== 'verdict' || p.index !== episode.stories.length - 1 || firstUnanswered(episode, p) !== -1) return;
                p.finishedAnswers = { ...p.answers };
                p.phase = 'results';
                save(p);
                renderResults();
                break;
            }
            case 'restart': openEpisode(episode, true); break;
            case 'next-episode': openEpisode(archive.episodes[archive.episodes.indexOf(episode) + 1]); break;
            case 'series': renderSeriesEnd(); break;
            case 'results': checkpoint('results'); renderResults(); break;
        }
    }

    function changePlayback(event) {
        const input = event.target;
        switch (input.name) {
            case 'music':
                musicEnabled = input.checked;
                updateMusic();
                break;
            case 'motion':
                reducedMotion = input.checked;
                dialog.classList.toggle('myst-reduced', reducedMotion);
                break;
            case 'voice':
                readAloud = input.checked;
                if (readAloud) speak();
                else synth.cancel();
                break;
        }
    }

    async function loadArchive() {
        const owner = dialog;
        setStage('<h2 class="myst-message" tabindex="-1">Das Folgenarchiv wird geöffnet …</h2>');
        try {
            const response = await fetch('/mystery/grumbrechtstrasse-episoden.json');
            if (!response.ok) throw new Error('Archive unavailable');
            const content = await response.json();
            if (dialog !== owner || !owner.open) return;
            archive = content;
            renderArchive();
        } catch {
            if (dialog === owner && owner.open) setStage(`<div class="myst-message"><h2 tabindex="-1">Das Archiv ist gerade nicht erreichbar.</h2>${button('Erneut versuchen', 'reload')}</div>`);
        }
    }

    function openShow(event) {
        if (dialog) return;
        opener = event.currentTarget;
        episode = null;
        dialog = document.createElement('dialog');
        dialog.className = 'myst-overlay';
        dialog.classList.toggle('myst-reduced', reducedMotion);
        dialog.setAttribute('aria-label', 'Grumbrechtstraße oder ausgedacht?');
        dialog.innerHTML = `<div class="myst-bar"><div class="myst-channel"><b>GOA</b><span>Nachtprogramm</span></div>
            <div class="myst-tools"><button type="button" data-action="archive" aria-label="Zur Folgenübersicht" hidden>Folgen</button>
            <details class="myst-settings"><summary>Wiedergabe</summary><div>
                <label><input type="checkbox" name="music" ${musicEnabled ? 'checked' : ''}>Atmosphärenmusik</label>
                ${synth && window.SpeechSynthesisUtterance ? '<label><input type="checkbox" name="voice">Mit Browserstimme vorlesen</label>' : ''}
                <label><input type="checkbox" name="motion" ${reducedMotion ? 'checked' : ''}>Bewegung reduzieren</label>
            </div></details>
            <button type="button" data-action="close" class="myst-close" aria-label="Sendung schließen">×</button></div></div>
            <div class="myst-stage"></div>`;
        stage = dialog.querySelector('.myst-stage');
        dialog.addEventListener('click', onAction);
        dialog.addEventListener('change', changePlayback);
        document.addEventListener('visibilitychange', updateMusic);
        dialog.addEventListener('close', () => {
            clearTimeout(transitionTimer);
            document.removeEventListener('visibilitychange', updateMusic);
            music?.pause();
            if (readAloud) synth.cancel();
            readAloud = false;
            spokenText = '';
            document.body.classList.remove('egg-open');
            dialog.remove();
            dialog = null;
            if (opener === logo && !logo.hasAttribute('tabindex')) logo.tabIndex = -1;
            opener.focus({ preventScroll: true });
        }, { once: true });
        document.body.append(dialog);
        document.body.classList.add('egg-open');
        dialog.showModal();
        if (archive) renderArchive();
        else loadArchive();
    }
});
