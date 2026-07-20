(function () {
  var appRoot = document.getElementById('app');
  var currentPageName = detectPageName();
  var userConfig = window.HMS_TIZEN_CONFIG || {};
  var apiBaseUrl = normalizeApiBaseUrl(userConfig.apiBaseUrl);
  var apiKey = userConfig.apiKey || getQueryParam('apiKey') || 'dev-local-key';
  var pageHandles = {
    searchStream: null,
    mediaInfoStream: null,
    torrentTimer: null
  };

  var state = {
    route: { name: currentPageName, params: detectRouteParams(currentPageName) },
    libraryQuery: getQueryParam('query') || '',
    libraryLoading: false,
    libraryError: '',
    uploadStatus: '',
    selectedUploadFile: null,
    uploadRequest: {
      title: '',
      type: 'movie',
      year: new Date().getFullYear(),
      description: ''
    },
    series: [],
    movies: [],
    selectedSeries: null,
    seasons: [],
    selectedSeason: null,
    episodes: [],
    movie: null,
    activeMedia: null,
    manifest: null,
    tracks: [],
    selectedCaption: 'off',
    mediaInfo: null,
    playbackError: '',
    search: {
      query: getQueryParam('q') || '',
      category: getQueryParam('category') === 'SERIES' ? 'SERIES' : 'MOVIE',
      sortBy: getQueryParam('sort') === 'size' ? 'size' : 'seeders',
      loading: false,
      hasSearched: false,
      error: '',
      importStatus: '',
      resultsBySource: {},
      importInFlight: {}
    },
    torrents: [],
    torrentLoading: false,
    torrentError: '',
    torrentStatus: '',
    torrentActions: {},
    metadataUi: {}
  };

  document.addEventListener('click', handleClick, false);
  document.addEventListener('submit', handleSubmit, false);
  document.addEventListener('change', handleChange, false);

  initializePage();

  function normalizeApiBaseUrl(rawValue) {
    var fromQuery = getQueryParam('apiBaseUrl');
    var value = rawValue || fromQuery || '';

    if (!value) {
      if (window.location.protocol === 'file:') {
        return 'http://localhost:8080';
      }
      return window.location.origin;
    }

    return String(value).replace(/\/+$/, '');
  }

  function getQueryParam(name) {
    var query = window.location.search || '';
    var escapedName = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    var match = query.match(new RegExp('[?&]' + escapedName + '=([^&]+)'));
    return match ? decodeURIComponent(match[1]) : '';
  }

  function initializePage() {
    closePageHandles();
    state.playbackError = '';
    state.mediaInfo = null;
    state.activeMedia = null;
    state.manifest = null;
    state.tracks = [];
    state.selectedCaption = 'off';
    state.torrentError = '';
    state.torrentStatus = '';
    markActiveNav();
    render();
    loadRouteData();
  }

  function detectPageName() {
    var bodyPage = document.body && document.body.getAttribute('data-page');
    var path = window.location.pathname || '';
    var fileName = path.split('/').pop() || 'index.html';

    if (bodyPage) {
      return bodyPage;
    }

    if (fileName === 'search.html') {
      return 'search';
    }
    if (fileName === 'torrents.html') {
      return 'torrents';
    }
    if (fileName === 'movie.html') {
      return 'movie';
    }
    if (fileName === 'series.html') {
      return 'series';
    }

    return 'home';
  }

  function detectRouteParams(pageName) {
    if (pageName === 'movie') {
      return { movieId: getQueryParam('movieId') || '' };
    }
    if (pageName === 'series') {
      return { seriesId: getQueryParam('seriesId') || '' };
    }
    return {};
  }

  function markActiveNav() {
    var links = document.querySelectorAll('[data-nav]');
    var i;
    for (i = 0; i < links.length; i += 1) {
      links[i].className = links[i].className.replace(/\s*active\b/g, '');
      if (links[i].getAttribute('data-nav') === state.route.name || (state.route.name === 'movie' && links[i].getAttribute('data-nav') === 'home') || (state.route.name === 'series' && links[i].getAttribute('data-nav') === 'home')) {
        links[i].className += ' active';
      }
    }
  }

  function loadRouteData() {
    if (state.route.name === 'home') {
      loadLibraryHome();
      return;
    }
    if (state.route.name === 'movie') {
      loadMovie(state.route.params.movieId);
      return;
    }
    if (state.route.name === 'series') {
      loadSeries(state.route.params.seriesId);
      return;
    }
    if (state.route.name === 'search') {
      render();
      if (state.search.query) {
        runAcquisitionSearch();
      }
      return;
    }
    if (state.route.name === 'torrents') {
      loadTorrentInfo();
    }
  }

  function closePageHandles() {
    if (pageHandles.searchStream) {
      pageHandles.searchStream.abort();
      pageHandles.searchStream = null;
    }
    if (pageHandles.mediaInfoStream) {
      pageHandles.mediaInfoStream.abort();
      pageHandles.mediaInfoStream = null;
    }
    if (pageHandles.torrentTimer) {
      window.clearTimeout(pageHandles.torrentTimer);
      pageHandles.torrentTimer = null;
    }
  }

  function render() {
    appRoot.innerHTML = renderPage();
    applyCaptionTrack();
  }

  function renderPage() {
    if (state.route.name === 'search') {
      return renderSearchPage();
    }
    if (state.route.name === 'movie') {
      return renderMoviePage();
    }
    if (state.route.name === 'series') {
      return renderSeriesPage();
    }
    if (state.route.name === 'torrents') {
      return renderTorrentPage();
    }
    return renderHomePage();
  }

  function renderHomePage() {
    var html = '';
    html += '<div class="page">';
    html += '<header class="hero"><h1>Home Media Server</h1><p>Search, ingest, and stream your movie and series library with subtitles.</p></header>';
    html += '<section class="panel controls">';
    html += '<div class="controls-header"><h2>Search Torrents</h2></div>';
    html += '<p class="muted">Torrent search now runs from a dedicated page.</p>';
    html += '<div class="torrent-shortcuts">';
    html += '<button type="button" data-action="go-search">Open Search Page</button> ';
    html += '<button type="button" class="secondary-button" data-action="go-torrents">Open Torrent Queue</button>';
    html += '</div></section>';
    html += '<section class="panel library">';
    html += '<div class="library-header"><h2>Media Library</h2></div>';
    html += '<div class="page-note">This version avoids modules, fetch streams, CSS grid, and newer syntax so it runs in Chromium 47-era browsers.</div>';

    if (state.uploadStatus) {
      html += '<p class="status">' + escapeHtml(state.uploadStatus) + '</p>';
    }
    if (state.libraryError) {
      html += '<p class="error">' + escapeHtml(state.libraryError) + '</p>';
    }
    if (state.libraryLoading) {
      html += '<p class="muted">Loading library...</p>';
    }

    html += '<div class="library-subsection">';
    html += '<h3>Search Library</h3>';
    html += '<form id="library-search-form" class="search-row">';
    html += '<input id="library-query" type="text" value="' + escapeAttribute(state.libraryQuery) + '" placeholder="Search media library">';
    html += '<button type="submit">Search</button>';
    html += '</form></div>';

    html += '<div class="library-subsection">';
    html += '<h3>Upload to Library</h3>';
    html += '<form id="upload-form" class="upload-grid">';
    html += '<div class="field"><input id="upload-title" type="text" value="' + escapeAttribute(state.uploadRequest.title) + '" placeholder="Upload title"></div>';
    html += '<div class="field field-right"><select id="upload-type"><option value="movie"' + selectedIf(state.uploadRequest.type, 'movie') + '>Movie</option><option value="series"' + selectedIf(state.uploadRequest.type, 'series') + '>Series</option></select></div>';
    html += '<div class="field"><input id="upload-year" type="number" value="' + escapeAttribute(String(state.uploadRequest.year || '')) + '" placeholder="Year"></div>';
    html += '<div class="field field-right"><input id="upload-file" type="file" accept="video/*"></div>';
    html += '<div class="field field-wide"><textarea id="upload-description" placeholder="Description">' + escapeHtml(state.uploadRequest.description) + '</textarea></div>';
    html += '<div class="field field-wide field-submit"><button type="submit">Upload</button></div>';
    html += '</form></div>';

    html += '<section class="library-subsection"><h3>Series</h3>';
    if (!state.series.length && !state.libraryLoading) {
      html += '<p class="muted">No series found.</p>';
    } else {
      html += renderCardCollection(state.series, 'open-series', 'Open Series');
    }
    html += '</section>';

    html += '<section class="library-subsection"><h3>Movies</h3>';
    if (!state.movies.length && !state.libraryLoading) {
      html += '<p class="muted">No movies found.</p>';
    } else {
      html += renderCardCollection(state.movies, 'open-movie', 'Open Movie');
    }
    html += '</section>';
    html += '</section></div>';
    return html;
  }

  function renderSearchPage() {
    var groups = sortResultGroups(state.search.resultsBySource, state.search.sortBy);
    var html = '';
    html += '<div class="page search-page">';
    html += '<header class="hero"><h1>Search Torrents</h1><p>Search torrent sources and send import requests to the stream service.</p></header>';
    html += '<section class="panel controls">';
    html += '<div class="controls-header"><h2>Search Torrents</h2></div>';
    html += '<div class="acquisition-search">';
    html += '<div class="acquisition-actions">';
    html += '<select id="search-category"><option value="MOVIE"' + selectedIf(state.search.category, 'MOVIE') + '>Movies</option><option value="SERIES"' + selectedIf(state.search.category, 'SERIES') + '>Series</option></select>';
    html += '<select id="search-sort"><option value="seeders"' + selectedIf(state.search.sortBy, 'seeders') + '>Sort by Seeders</option><option value="size"' + selectedIf(state.search.sortBy, 'size') + '>Sort by Size</option></select>';
    html += '</div>';
    html += '<form id="acquisition-search-form" class="acquisition-search-box">';
    html += '<input id="search-query" type="text" value="' + escapeAttribute(state.search.query) + '" placeholder="Search torrent sources" aria-label="Search torrent sources">';
    html += '<button type="submit">Search Sources</button>';
    html += '</form></div>';
    if (state.search.error) {
      html += '<p class="error">' + escapeHtml(state.search.error) + '</p>';
    }
    if (state.search.importStatus) {
      html += '<p class="status">' + escapeHtml(state.search.importStatus) + '</p>';
    }
    html += '</section>';
    html += '<section class="panel search-results-panel">';
    html += '<h2>' + (state.search.query ? 'Results for &quot;' + escapeHtml(state.search.query) + '&quot;' : 'Search Results') + '</h2>';
    if (state.search.loading) {
      html += '<p class="muted">Searching sources...</p>';
    } else if (state.search.hasSearched && !groups.length) {
      html += '<p class="muted">No results found.</p>';
    } else if (!state.search.hasSearched) {
      html += '<p class="muted">Click Search Sources to run a search.</p>';
    }

    if (groups.length) {
      html += '<div class="search-results-groups">';
      html += renderSearchGroups(groups);
      html += '</div>';
    }
    html += '</section></div>';
    return html;
  }

  function renderMoviePage() {
    var movie = state.movie;
    var html = '';
    html += '<div class="page">';
    html += '<header class="hero"><h1>' + escapeHtml(movie ? movie.title : 'Movie') + '</h1><p>' + escapeHtml(movie && (movie.plotSummary || movie.description) ? (movie.plotSummary || movie.description) : 'No plot summary available.') + '</p></header>';
    if (!movie) {
      html += '<p class="error">Movie not found.</p></div>';
      return html;
    }
    if (state.playbackError) {
      html += '<p class="error">' + escapeHtml(state.playbackError) + '</p>';
    }
    html += '<section class="panel overview-section">';
    html += '<div class="detail-toolbar"><h2>Movie Details</h2><button type="button" class="secondary-button" data-action="go-home">Back to Library</button></div>';
    html += renderMediaCard(movie, { action: 'play-movie', actionLabel: 'Stream Movie', idValue: movie.mediaId || movie.id, idName: 'mediaId' });
    html += renderDetailGrid(buildMovieEntries(movie, 'Movie ID'));
    html += '</section>';
    html += renderPlayerSection('Movie Stream Window', 'Select this movie to begin streaming.');
    html += '</div>';
    return html;
  }

  function renderSeriesPage() {
    var html = '';
    var series = state.selectedSeries;
    html += '<div class="page">';
    html += '<header class="hero"><h1>' + escapeHtml(series ? series.title : 'Series') + '</h1><p>' + escapeHtml(series && (series.plotSummary || series.description) ? (series.plotSummary || series.description) : 'No plot summary available.') + '</p></header>';
    if (!series) {
      html += '<p class="error">Series not found.</p></div>';
      return html;
    }
    if (state.playbackError) {
      html += '<p class="error">' + escapeHtml(state.playbackError) + '</p>';
    }
    html += '<section class="panel overview-section">';
    html += '<div class="detail-toolbar"><h2>Series Details</h2><button type="button" class="secondary-button" data-action="go-home">Back to Library</button></div>';
    html += renderMediaCard(series, { action: '', actionLabel: '', showAction: false });
    html += renderDetailGrid(buildSeriesEntries(series, 'Series ID'));
    html += '</section>';
    html += '<section class="panel library-subsection">';
    html += '<h3>Seasons</h3>';
    if (!state.seasons.length) {
      html += '<p class="muted">No seasons found for this series.</p>';
    } else {
      html += '<div class="season-buttons">';
      html += renderSeasonButtons(state.seasons, state.selectedSeason ? (state.selectedSeason.seasonId || state.selectedSeason.id) : '');
      html += '</div>';
    }
    if (state.selectedSeason) {
      html += renderDetailGrid(buildSeriesEntries(state.selectedSeason, 'Season ID'));
    }
    html += '<h3>Episodes</h3>';
    if (!state.episodes.length) {
      html += '<p class="muted">No episodes found for this season.</p>';
    } else {
      html += renderCardCollection(state.episodes, 'play-episode', 'Stream Episode');
    }
    html += '</section>';
    html += renderPlayerSection('Episode Stream Window', 'Select an episode to begin streaming.');
    html += '</div>';
    return html;
  }

  function renderTorrentPage() {
    var html = '';
    html += '<div class="page torrent-info-page">';
    html += '<header class="hero"><h1>Torrent Queue</h1><p>Inspect active torrents and control pause, resume, and delete operations.</p></header>';
    html += '<section class="panel controls">';
    html += '<div class="torrent-toolbar"><h2>TorrentInfoResponse List</h2><button type="button" data-action="refresh-torrents"' + (state.torrentLoading ? ' disabled="disabled"' : '') + '>Refresh</button></div>';
    if (state.torrentStatus) {
      html += '<p class="status">' + escapeHtml(state.torrentStatus) + '</p>';
    }
    if (state.torrentError) {
      html += '<p class="error">' + escapeHtml(state.torrentError) + '</p>';
    }
    if (state.torrentLoading) {
      html += '<p class="muted">Loading torrent information...</p>';
    } else if (!state.torrents.length) {
      html += '<p class="muted">No torrents found.</p>';
    } else {
      html += '<div class="torrent-list">';
      html += renderTorrentList(state.torrents);
      html += '</div>';
    }
    html += '</section></div>';
    return html;
  }

  function renderCardCollection(items, actionName, actionLabel) {
    var html = '<div class="cards">';
    var i;
    for (i = 0; i < items.length; i += 1) {
      html += '<div class="card-cell">';
      html += renderMediaCard(items[i], { action: actionName, actionLabel: actionLabel, idValue: resolveMediaId(items[i]), idName: 'mediaId' });
      html += '</div>';
    }
    html += '</div>';
    return html;
  }

  function renderMediaCard(item, options) {
    var metadata = getCurrentMetadata(item);
    var ui = ensureMetadataUi(item);
    var poster = normalizePosterSrc(item);
    var html = '';
    var showAction = options.showAction !== false && options.action;

    html += '<article class="media-card">';
    html += '<div class="poster-frame">';
    if (poster) {
      html += '<img class="poster" src="' + escapeAttribute(resolveMediaUrl(poster)) + '" alt="' + escapeAttribute(metadata.title || item.title || 'Poster') + '">';
    } else {
      html += '<span class="empty-poster">Poster unavailable</span>';
    }
    html += '</div>';
    html += '<div class="media-card-body">';
    html += '<div class="meta-row"><span>' + escapeHtml(capitalize(item.type || 'unknown')) + '</span><span>' + escapeHtml(String(item.year || formatYear(item.releaseDate) || 'N/A')) + '</span></div>';
    html += '<h3>' + escapeHtml(metadata.title || item.title || 'Untitled') + '</h3>';
    html += '<p class="summary">' + escapeHtml(metadata.plotSummary || item.plotSummary || item.description || 'No description available.') + '</p>';
    html += '<div class="card-details">';
    html += '<span>Rating: ' + escapeHtml(formatRating(metadata.rating !== '' ? metadata.rating : item.rating)) + '</span>';
    html += '<span>Release: ' + escapeHtml(formatReleaseDate(metadata.airDate || item.releaseDate) || 'N/A') + '</span>';
    html += '<span>ID: ' + escapeHtml(String(resolveMediaId(item) || 'N/A')) + '</span>';
    html += '</div>';

    if (metadata.metaDataId) {
      html += '<div class="metadata-actions">';
      if (!ui.editing) {
        html += '<button type="button" class="secondary-button" data-action="edit-metadata" data-metadata-id="' + escapeAttribute(metadata.metaDataId) + '">Edit Metadata</button> ';
        html += '<button type="button" class="secondary-button" data-action="metadata-search" data-metadata-id="' + escapeAttribute(metadata.metaDataId) + '">Search Metadata Again</button>';
      }
      html += '</div>';
      if (ui.editing) {
        html += renderMetadataEditor(item, metadata, ui);
      }
      if (ui.statusMessage) {
        html += '<p class="status metadata-message">' + escapeHtml(ui.statusMessage) + '</p>';
      }
      if (ui.error) {
        html += '<p class="error metadata-message">' + escapeHtml(ui.error) + '</p>';
      }
    }

    if (showAction) {
      html += '<button type="button" data-action="' + escapeAttribute(options.action) + '" data-media-id="' + escapeAttribute(String(options.idValue || '')) + '">' + escapeHtml(options.actionLabel) + '</button>';
    }
    html += '</div></article>';
    return html;
  }

  function renderMetadataEditor(item, metadata, ui) {
    var html = '';
    html += '<form class="metadata-editor" data-form="metadata-editor" data-metadata-id="' + escapeAttribute(metadata.metaDataId) + '">';
    html += '<label>Metadata ID<input type="text" value="' + escapeAttribute(metadata.metaDataId) + '" disabled="disabled"></label>';
    html += '<label>Title<input type="text" name="title" value="' + escapeAttribute(ui.draft.title) + '"></label>';
    html += '<label>Plot Summary<textarea name="plotSummary">' + escapeHtml(ui.draft.plotSummary) + '</textarea></label>';
    html += '<label>Air Date<input type="date" name="airDate" value="' + escapeAttribute(ui.draft.airDate) + '"></label>';
    html += '<label>Rating<input type="number" name="rating" min="0" max="10" step="0.1" value="' + escapeAttribute(ui.draft.rating) + '"></label>';
    html += '<label>Status<select name="status">';
    html += renderMetadataStatusOptions(ui.draft.status);
    html += '</select></label>';
    html += '<label>Message<textarea name="message">' + escapeHtml(ui.draft.message) + '</textarea></label>';
    html += '<div class="metadata-editor-actions">';
    html += '<button type="submit"' + (ui.saving ? ' disabled="disabled"' : '') + '>' + (ui.saving ? 'Saving...' : 'Save Metadata') + '</button> ';
    html += '<button type="button" class="secondary-button" data-action="cancel-metadata" data-metadata-id="' + escapeAttribute(metadata.metaDataId) + '">Cancel</button>';
    html += '</div></form>';
    return html;
  }

  function renderMetadataStatusOptions(selectedValue) {
    var options = ['PENDING', 'COMPLETE', 'INCOMPLETE', 'NOT_FOUND', 'ERROR'];
    var html = '';
    var i;
    for (i = 0; i < options.length; i += 1) {
      html += '<option value="' + options[i] + '"' + selectedIf(selectedValue, options[i]) + '>' + options[i] + '</option>';
    }
    return html;
  }

  function renderSearchGroups(groups) {
    var html = '';
    var i;
    var j;
    for (i = 0; i < groups.length; i += 1) {
      html += '<article class="search-source-group"><h3>' + escapeHtml(groups[i].source) + '</h3><ul class="search-results-list">';
      for (j = 0; j < groups[i].items.length; j += 1) {
        html += renderSearchResult(groups[i].items[j]);
      }
      html += '</ul></article>';
    }
    return html;
  }

  function renderSearchResult(item) {
    var key = item.magnetLink || item.sourceUrl || item.title || '';
    var loading = state.search.importInFlight[key];
    var html = '';
    html += '<li class="search-result-item">';
    html += '<div class="search-result-row"><strong>' + escapeHtml(item.title || 'Untitled Result') + '</strong></div>';
    html += '<div class="search-result-row search-result-meta">';
    html += '<span>Seeders: ' + escapeHtml(String(item.seeders || 'N/A')) + '</span>';
    html += '<span>Leechers: ' + escapeHtml(String(item.leechers || 'N/A')) + '</span>';
    html += '<span>Size: ' + escapeHtml(String(item.size || 'N/A')) + '</span>';
    html += '<span>Category: ' + escapeHtml(String(item.category || state.search.category)) + '</span>';
    html += '</div>';
    html += '<div class="search-result-actions">';
    html += '<button type="button" class="secondary-button" data-action="use-result-title" data-title="' + escapeAttribute(item.title || '') + '">Use Title</button> ';
    html += '<button type="button" data-action="import-result" data-result-key="' + escapeAttribute(key) + '"' + (loading ? ' disabled="disabled"' : '') + '>' + (loading ? 'Importing...' : 'Import') + '</button>';
    html += '</div></li>';
    return html;
  }

  function renderTorrentList(torrents) {
    var html = '';
    var i;
    for (i = 0; i < torrents.length; i += 1) {
      html += renderTorrentCard(torrents[i]);
    }
    return html;
  }

  function renderTorrentCard(torrent) {
    var progress = getCompletionPercent(torrent);
    var busy = state.torrentActions[torrent.infoHash];
    var html = '';
    html += '<article class="torrent-card">';
    html += '<div class="torrent-card-header"><h3>' + escapeHtml(torrent.name || 'Unnamed Torrent') + '</h3><span class="queue-chip">Queue #' + escapeHtml(String(torrent.queuePosition || '0')) + '</span></div>';
    html += '<p class="torrent-hash">' + escapeHtml(torrent.infoHash || '') + '</p>';
    html += '<div class="progress-wrap"><div class="progress-track"><div class="progress-fill" style="width:' + progress + '%"></div></div><span>' + progress.toFixed(1) + '%</span></div>';
    html += '<div class="torrent-grid">';
    html += '<p><strong>Total:</strong> ' + escapeHtml(formatBytes(torrent.totalSize)) + '</p>';
    html += '<p><strong>Downloaded:</strong> ' + escapeHtml(formatBytes(torrent.downloadedSize)) + '</p>';
    html += '<p><strong>Download:</strong> ' + escapeHtml(formatSpeed(torrent.downloadSpeed)) + '</p>';
    html += '<p><strong>Upload:</strong> ' + escapeHtml(formatSpeed(torrent.uploadSpeed)) + '</p>';
    html += '<p><strong>Peers:</strong> ' + escapeHtml(String(displayValue(torrent.numPeers))) + '</p>';
    html += '<p><strong>Import Status:</strong> ' + escapeHtml(formatImportMediaStatus(torrent.importMediaStatus)) + '</p>';
    html += '</div>';
    html += '<div class="torrent-actions">';
    html += '<button type="button" class="secondary-button" data-action="pause-torrent" data-info-hash="' + escapeAttribute(torrent.infoHash || '') + '"' + (busy ? ' disabled="disabled"' : '') + '>Pause</button> ';
    html += '<button type="button" class="secondary-button" data-action="resume-torrent" data-info-hash="' + escapeAttribute(torrent.infoHash || '') + '"' + (busy ? ' disabled="disabled"' : '') + '>Resume</button> ';
    html += '<button type="button" class="danger-button" data-action="delete-torrent" data-info-hash="' + escapeAttribute(torrent.infoHash || '') + '"' + (busy ? ' disabled="disabled"' : '') + '>Delete</button>';
    html += '</div></article>';
    return html;
  }

  function renderPlayerSection(title, emptyText) {
    var html = '';
    var active = state.activeMedia;
    html += '<section class="panel player-section">';
    html += '<h2>' + escapeHtml(title) + '</h2>';
    if (!active) {
      html += '<p>' + escapeHtml(emptyText) + '</p>';
    } else {
      html += '<h3>' + escapeHtml(active.title || 'Playback') + '</h3>';
      html += '<p class="player-meta">';
      html += '<span>Type: ' + escapeHtml(displayValue(active.type)) + '</span>';
      html += '<span>Release: ' + escapeHtml(formatReleaseDate(active.releaseDate) || 'N/A') + '</span>';
      html += '<span>Rating: ' + escapeHtml(formatRating(active.rating)) + '</span>';
      if (active.seasonNumber !== null && active.seasonNumber !== undefined) {
        html += '<span>Season #: ' + escapeHtml(displayValue(active.seasonNumber)) + '</span>';
      }
      if (active.episodeNumber !== null && active.episodeNumber !== undefined) {
        html += '<span>Episode #: ' + escapeHtml(displayValue(active.episodeNumber)) + '</span>';
      }
      html += '</p>';
      html += '<p class="player-summary">' + escapeHtml(active.plotSummary || active.description || 'No description available.') + '</p>';
      html += '<div class="video-wrap"><video id="media-player" controls="controls" preload="metadata" src="' + escapeAttribute(resolveMediaUrl((state.manifest && state.manifest.playbackUrl) || active.streamUrl || '')) + '">';
      html += renderTrackTags(state.tracks);
      html += '</video></div>';
      html += '<div class="caption-controls"><label for="caption-select">Closed Captions</label><select id="caption-select"><option value="off">Off</option>' + renderCaptionOptions(state.tracks, state.selectedCaption) + '</select></div>';
      if (state.mediaInfo) {
        html += renderMediaInfoPanel(state.mediaInfo);
      }
      html += renderDetailGrid(buildSeriesEntries(active, 'Playback ID'));
    }
    html += '</section>';
    return html;
  }

  function renderTrackTags(tracks) {
    var html = '';
    var i;
    for (i = 0; i < tracks.length; i += 1) {
      html += '<track kind="subtitles" label="' + escapeAttribute(tracks[i].label || tracks[i].language || 'Subtitle') + '" src="' + escapeAttribute(tracks[i].url || '') + '" srclang="' + escapeAttribute(tracks[i].language || 'en') + '">';
    }
    return html;
  }

  function renderCaptionOptions(tracks, selectedCaption) {
    var html = '';
    var i;
    for (i = 0; i < tracks.length; i += 1) {
      html += '<option value="' + escapeAttribute(tracks[i].language || 'en') + '"' + selectedIf(selectedCaption, tracks[i].language || 'en') + '>' + escapeHtml(tracks[i].label || tracks[i].language || 'Subtitle') + '</option>';
    }
    return html;
  }

  function renderMediaInfoPanel(mediaInfo) {
    var progress = getMediaDownloadPercent(mediaInfo);
    var html = '';
    html += '<div class="media-info-panel">';
    html += '<div class="progress-wrap"><div class="progress-track"><div class="progress-fill" style="width:' + progress + '%"></div></div><strong>' + progress.toFixed(1) + '%</strong></div>';
    html += renderDetailGrid([
      { label: 'Stream mediaItemId', value: displayValue(mediaInfo.mediaItemId) },
      { label: 'File size', value: formatBytes(mediaInfo.fileSize) },
      { label: 'Bytes downloaded', value: formatBytes(mediaInfo.bytesDownloaded) },
      { label: 'Delta bytes downloaded', value: formatSpeed(mediaInfo.deltaBytesDownloaded) },
      { label: 'Required download rate', value: formatSpeed(mediaInfo.requiredDownloadRate) },
      { label: 'Import media status', value: formatImportMediaStatus(mediaInfo.importMediaStatus) }
    ]);
    html += '</div>';
    return html;
  }

  function renderDetailGrid(entries) {
    var html = '<div class="detail-grid">';
    var i;
    for (i = 0; i < entries.length; i += 1) {
      html += '<div class="detail-row"><strong>' + escapeHtml(entries[i].label) + ':</strong> <span>' + escapeHtml(String(entries[i].value)) + '</span></div>';
    }
    html += '</div>';
    return html;
  }

  function renderSeasonButtons(seasons, selectedId) {
    var html = '';
    var i;
    var seasonId;
    for (i = 0; i < seasons.length; i += 1) {
      seasonId = seasons[i].seasonId || seasons[i].id;
      html += '<button type="button" class="secondary-button' + (String(selectedId) === String(seasonId) ? ' season-active' : '') + '" data-action="open-season" data-season-id="' + escapeAttribute(String(seasonId)) + '">' + escapeHtml(seasons[i].title || seasons[i].name || ('Season ' + displayValue(seasons[i].seasonNumber))) + '</button>';
    }
    return html;
  }

  function handleSubmit(event) {
    var form = event.target;
    if (!form || !form.id && form.getAttribute('data-form') !== 'metadata-editor') {
      return;
    }

    if (form.id === 'library-search-form') {
      event.preventDefault();
      state.libraryQuery = getInputValue('library-query');
      replaceCurrentQuery({ query: state.libraryQuery });
      loadLibraryHome();
      return;
    }

    if (form.id === 'upload-form') {
      event.preventDefault();
      syncUploadInputs();
      uploadMedia();
      return;
    }

    if (form.id === 'acquisition-search-form') {
      event.preventDefault();
      state.search.query = getInputValue('search-query');
      replaceCurrentQuery({ q: state.search.query, category: state.search.category, sort: state.search.sortBy });
      runAcquisitionSearch();
      return;
    }

    if (form.getAttribute('data-form') === 'metadata-editor') {
      event.preventDefault();
      saveMetadata(form.getAttribute('data-metadata-id'), form);
    }
  }

  function handleChange(event) {
    var target = event.target;
    if (!target) {
      return;
    }

    if (target.id === 'upload-file') {
      state.selectedUploadFile = target.files && target.files[0] ? target.files[0] : null;
      return;
    }

    if (target.id === 'search-category') {
      state.search.category = target.value || 'MOVIE';
      replaceCurrentQuery({ q: state.search.query, category: state.search.category, sort: state.search.sortBy });
      return;
    }

    if (target.id === 'search-sort') {
      state.search.sortBy = target.value || 'seeders';
      replaceCurrentQuery({ q: state.search.query, category: state.search.category, sort: state.search.sortBy });
      render();
      return;
    }

    if (target.id === 'caption-select') {
      state.selectedCaption = target.value || 'off';
      applyCaptionTrack();
    }
  }

  function handleClick(event) {
    var actionTarget = findActionTarget(event.target);
    if (!actionTarget) {
      return;
    }

    var action = actionTarget.getAttribute('data-action');
    if (!action) {
      return;
    }

    if (action === 'go-home') {
      navigateToPage('index.html');
      return;
    }
    if (action === 'go-search') {
      navigateToPage('search.html', { q: state.search.query, category: state.search.category, sort: state.search.sortBy });
      return;
    }
    if (action === 'go-torrents') {
      navigateToPage('torrents.html');
      return;
    }
    if (action === 'open-series') {
      navigateToPage('series.html', { seriesId: actionTarget.getAttribute('data-media-id') });
      return;
    }
    if (action === 'open-movie') {
      navigateToPage('movie.html', { movieId: actionTarget.getAttribute('data-media-id') });
      return;
    }
    if (action === 'open-season') {
      openSeason(actionTarget.getAttribute('data-season-id'));
      return;
    }
    if (action === 'play-episode') {
      startPlayback(findEpisodeById(actionTarget.getAttribute('data-media-id')));
      return;
    }
    if (action === 'play-movie') {
      startPlayback(state.movie);
      return;
    }
    if (action === 'use-result-title') {
      state.search.query = actionTarget.getAttribute('data-title') || '';
      replaceCurrentQuery({ q: state.search.query, category: state.search.category, sort: state.search.sortBy });
      render();
      return;
    }
    if (action === 'import-result') {
      importSearchResult(actionTarget.getAttribute('data-result-key'));
      return;
    }
    if (action === 'refresh-torrents') {
      loadTorrentInfo();
      return;
    }
    if (action === 'pause-torrent') {
      runTorrentAction('pause', actionTarget.getAttribute('data-info-hash'), pauseTorrent);
      return;
    }
    if (action === 'resume-torrent') {
      runTorrentAction('resume', actionTarget.getAttribute('data-info-hash'), resumeTorrent);
      return;
    }
    if (action === 'delete-torrent') {
      runTorrentAction('delete', actionTarget.getAttribute('data-info-hash'), deleteTorrent);
      return;
    }
    if (action === 'edit-metadata') {
      beginMetadataEdit(actionTarget.getAttribute('data-metadata-id'));
      return;
    }
    if (action === 'cancel-metadata') {
      cancelMetadataEdit(actionTarget.getAttribute('data-metadata-id'));
      return;
    }
    if (action === 'metadata-search') {
      requestMetadataRefresh(actionTarget.getAttribute('data-metadata-id'));
    }
  }

  function findActionTarget(element) {
    while (element && element !== document.body) {
      if (element.getAttribute && element.getAttribute('data-action')) {
        return element;
      }
      element = element.parentNode;
    }
    return null;
  }

  function syncUploadInputs() {
    state.uploadRequest.title = getInputValue('upload-title');
    state.uploadRequest.type = getInputValue('upload-type') || 'movie';
    state.uploadRequest.year = getInputValue('upload-year');
    state.uploadRequest.description = getInputValue('upload-description');
  }

  function loadLibraryHome() {
    state.libraryLoading = true;
    state.libraryError = '';
    render();

    Promise.all([
      searchCatalogSeries(state.libraryQuery),
      searchCatalogMovies(state.libraryQuery)
    ]).then(function (results) {
      state.series = normalizeArray(results[0]).map(normalizeSeriesItem);
      state.movies = normalizeArray(results[1]).map(function (item) {
        return normalizeMovieItem(item);
      });
      state.libraryLoading = false;
      render();
    }, function (error) {
      state.libraryLoading = false;
      state.libraryError = getErrorMessage(error, 'Failed to load media library.');
      render();
    });
  }

  function uploadMedia() {
    if (!state.uploadRequest.title) {
      state.libraryError = 'Title is required to upload media.';
      render();
      return;
    }
    if (!state.selectedUploadFile) {
      state.libraryError = 'A video file is required for upload.';
      render();
      return;
    }

    state.libraryError = '';
    state.uploadStatus = 'Uploading file to stream service...';
    render();

    uploadMediaFile(state.selectedUploadFile, {
      title: state.uploadRequest.title,
      type: state.uploadRequest.type,
      year: state.uploadRequest.year,
      description: state.uploadRequest.description
    }).then(function () {
      state.uploadStatus = 'Upload complete: ' + state.selectedUploadFile.name;
      state.selectedUploadFile = null;
      state.uploadRequest.title = '';
      state.uploadRequest.description = '';
      state.uploadRequest.year = new Date().getFullYear();
      loadLibraryHome();
    }, function (error) {
      state.uploadStatus = '';
      state.libraryError = getErrorMessage(error, 'Upload failed.');
      render();
    });
  }

  function loadMovie(movieId) {
    state.movie = null;
    state.playbackError = '';
    render();
    searchCatalogMovies('').then(function (movies) {
      var list = normalizeArray(movies);
      var i;
      for (i = 0; i < list.length; i += 1) {
        if (String(resolveMediaId(list[i])) === String(movieId)) {
          state.movie = normalizeMovieItem(list[i]);
          render();
          return;
        }
      }
      state.movie = normalizeMovieItem({ mediaId: movieId, title: 'Movie', type: 'movie' });
      render();
    }, function (error) {
      state.movie = normalizeMovieItem({ mediaId: movieId, title: 'Movie', type: 'movie' });
      state.playbackError = getErrorMessage(error, 'Failed to load movie metadata.');
      render();
    });
  }

  function loadSeries(seriesId) {
    Promise.all([
      searchCatalogSeries(''),
      searchCatalogSeasons(seriesId)
    ]).then(function (results) {
      var seriesList = normalizeArray(results[0]);
      var i;
      var matched = null;
      for (i = 0; i < seriesList.length; i += 1) {
        if (String(seriesList[i].seriesId || seriesList[i].id) === String(seriesId)) {
          matched = seriesList[i];
          break;
        }
      }
      state.selectedSeries = normalizeSeriesItem(matched || { seriesId: seriesId, title: 'Series', type: 'series' });
      state.seasons = normalizeArray(results[1]).map(function (item) {
        return normalizeSeasonItem(item, state.selectedSeries);
      }).sort(function (left, right) {
        return (left.seasonNumber || 0) - (right.seasonNumber || 0);
      });
      state.selectedSeason = state.seasons.length ? state.seasons[0] : null;
      render();
      if (state.selectedSeason) {
        openSeason(state.selectedSeason.seasonId || state.selectedSeason.id);
      }
    }, function (error) {
      state.playbackError = getErrorMessage(error, 'Failed to load series.');
      render();
    });
  }

  function openSeason(seasonId) {
    var i;
    var targetSeason = null;
    for (i = 0; i < state.seasons.length; i += 1) {
      if (String(state.seasons[i].seasonId || state.seasons[i].id) === String(seasonId)) {
        targetSeason = state.seasons[i];
        break;
      }
    }
    if (!targetSeason || !state.selectedSeries) {
      return;
    }

    state.selectedSeason = targetSeason;
    state.episodes = [];
    state.mediaInfo = null;
    state.activeMedia = null;
    state.manifest = null;
    state.tracks = [];
    render();

    searchCatalogEpisodes(state.selectedSeries.seriesId, targetSeason.seasonId || targetSeason.id, '').then(function (episodes) {
      state.episodes = normalizeArray(episodes).map(normalizeEpisodeItem);
      render();
    }, function (error) {
      state.playbackError = getErrorMessage(error, 'Failed to load episodes.');
      render();
    });
  }

  function startPlayback(item) {
    if (!item) {
      return;
    }
    if (pageHandles.mediaInfoStream) {
      pageHandles.mediaInfoStream.abort();
      pageHandles.mediaInfoStream = null;
    }
    state.activeMedia = item;
    state.selectedCaption = 'off';
    state.mediaInfo = null;
    state.playbackError = '';
    state.manifest = null;
    state.tracks = [];
    render();

    var mediaItemId = resolveMediaId(item);
    openMediaInfoStream(mediaItemId);
    streamManifest(mediaItemId, item.streamUrl).then(function (manifest) {
      var captions = normalizeArray(manifest && manifest.captions);
      var i;
      state.manifest = manifest || null;
      state.tracks = [];
      for (i = 0; i < captions.length; i += 1) {
        state.tracks.push({
          language: captions[i].language || 'en',
          label: captions[i].label || captions[i].language || 'Subtitle',
          url: streamCaptionsUrl(mediaItemId, captions[i].language || 'en')
        });
      }
      render();
    }, function (error) {
      state.playbackError = getErrorMessage(error, 'Failed to start playback.');
      render();
    });
  }

  function openMediaInfoStream(mediaItemId) {
    if (!mediaItemId) {
      return;
    }
    pageHandles.mediaInfoStream = openSseRequest(apiUrl('/api/stream/torrent/media/infostream?mediaItemId=' + encodeURIComponent(mediaItemId)), {
      onUpdate: function (update) {
        state.mediaInfo = update;
        render();
      },
      onError: function (message) {
        state.playbackError = String(message || 'Failed to open media info stream.');
        render();
      }
    });
  }

  function applyCaptionTrack() {
    var player = document.getElementById('media-player');
    var select = document.getElementById('caption-select');
    var selected = select ? select.value : state.selectedCaption;
    var tracks;
    var i;
    if (!player || !player.textTracks) {
      return;
    }
    state.selectedCaption = selected || 'off';
    tracks = player.textTracks;
    for (i = 0; i < tracks.length; i += 1) {
      tracks[i].mode = state.selectedCaption !== 'off' && tracks[i].language === state.selectedCaption ? 'showing' : 'disabled';
    }
  }

  function runAcquisitionSearch() {
    if (!state.search.query) {
      state.search.hasSearched = false;
      state.search.error = '';
      state.search.resultsBySource = {};
      render();
      return;
    }

    if (pageHandles.searchStream) {
      pageHandles.searchStream.abort();
      pageHandles.searchStream = null;
    }

    state.search.loading = true;
    state.search.hasSearched = true;
    state.search.error = '';
    state.search.importStatus = '';
    state.search.resultsBySource = {};
    render();

    pageHandles.searchStream = searchAcquisitionStream(state.search.query, state.search.category, {
      onItem: function (item) {
        if (!item || !item.title) {
          return;
        }
        appendSearchResult(item);
        render();
      },
      onError: function (message) {
        state.search.error = String(message || 'Search failed.');
        render();
      },
      onDone: function () {
        state.search.loading = false;
        render();
      }
    });
  }

  function appendSearchResult(item) {
    var normalized = clone(item);
    var source = normalized.source || 'Unknown Source';
    var existing = state.search.resultsBySource[source] || [];
    var i;
    for (i = 0; i < existing.length; i += 1) {
      if ((normalized.magnetLink && normalized.magnetLink === existing[i].magnetLink) || (normalized.sourceUrl && normalized.sourceUrl === existing[i].sourceUrl)) {
        return;
      }
    }
    normalized.category = normalized.category || state.search.category;
    existing.push(normalized);
    state.search.resultsBySource[source] = existing;
  }

  function importSearchResult(resultKey) {
    var result = findSearchResultByKey(resultKey);
    if (!result || !result.title || !result.magnetLink) {
      state.search.error = 'Selected result is missing title or magnet link.';
      render();
      return;
    }

    state.search.importInFlight[resultKey] = true;
    state.search.error = '';
    render();

    importStreamMedia({
      title: result.title,
      magnetLink: result.magnetLink,
      category: result.category || state.search.category
    }).then(function () {
      delete state.search.importInFlight[resultKey];
      state.search.importStatus = 'Import request created for "' + result.title + '".';
      render();
    }, function (error) {
      delete state.search.importInFlight[resultKey];
      state.search.error = getErrorMessage(error, 'Import request failed.');
      render();
    });
  }

  function loadTorrentInfo() {
    state.torrentLoading = true;
    state.torrentError = '';
    render();

    getTorrentInfo().then(function (response) {
      state.torrents = normalizeArray(response);
      state.torrentLoading = false;
      render();
      scheduleTorrentRefresh();
    }, function (error) {
      state.torrentLoading = false;
      state.torrentError = getErrorMessage(error, 'Failed to load torrent information.');
      render();
      scheduleTorrentRefresh();
    });
  }

  function scheduleTorrentRefresh() {
    if (state.route.name !== 'torrents') {
      return;
    }
    if (pageHandles.torrentTimer) {
      window.clearTimeout(pageHandles.torrentTimer);
    }
    pageHandles.torrentTimer = window.setTimeout(function () {
      loadTorrentInfo();
    }, 3000);
  }

  function runTorrentAction(actionName, infoHash, actionFn) {
    if (!infoHash || state.torrentActions[infoHash]) {
      return;
    }

    state.torrentActions[infoHash] = true;
    state.torrentError = '';
    state.torrentStatus = '';
    render();

    actionFn(infoHash).then(function (ok) {
      state.torrentActions[infoHash] = false;
      if (!ok) {
        state.torrentError = 'Unable to ' + actionName + ' torrent.';
      } else {
        state.torrentStatus = 'Torrent ' + actionName + ' request sent.';
      }
      render();
      loadTorrentInfo();
    }, function (error) {
      state.torrentActions[infoHash] = false;
      state.torrentError = getErrorMessage(error, 'Failed to ' + actionName + ' torrent.');
      render();
    });
  }

  function beginMetadataEdit(metaDataId) {
    var item = findItemByMetadataId(metaDataId);
    var ui = ensureMetadataUi(item);
    ui.editing = true;
    ui.error = '';
    ui.statusMessage = '';
    ui.draft = getCurrentMetadata(item);
    render();
  }

  function cancelMetadataEdit(metaDataId) {
    var item = findItemByMetadataId(metaDataId);
    var ui = ensureMetadataUi(item);
    ui.editing = false;
    ui.error = '';
    ui.statusMessage = '';
    ui.draft = getCurrentMetadata(item);
    render();
  }

  function saveMetadata(metaDataId, form) {
    var item = findItemByMetadataId(metaDataId);
    var ui = ensureMetadataUi(item);
    var ratingValue = getFormValue(form, 'rating');
    var parsedRating = parseFloat(ratingValue);
    ui.saving = true;
    ui.error = '';
    ui.statusMessage = '';
    ui.draft = {
      metaDataId: metaDataId,
      title: getFormValue(form, 'title'),
      plotSummary: getFormValue(form, 'plotSummary'),
      airDate: getFormValue(form, 'airDate'),
      rating: ratingValue,
      status: getFormValue(form, 'status') || 'PENDING',
      message: getFormValue(form, 'message')
    };
    render();

    updateMetadata(metaDataId, {
      metaDataId: metaDataId,
      title: ui.draft.title || '',
      plotSummary: ui.draft.plotSummary || '',
      airDate: ui.draft.airDate || null,
      rating: isFinite(parsedRating) ? parsedRating : null,
      status: ui.draft.status || 'PENDING',
      message: ui.draft.message || ''
    }).then(function (updated) {
      ui.saving = false;
      ui.editing = false;
      ui.statusMessage = 'Metadata updated.';
      patchItemMetadata(metaDataId, updated);
      render();
    }, function (error) {
      ui.saving = false;
      ui.error = getErrorMessage(error, 'Failed to update metadata.');
      render();
    });
  }

  function requestMetadataRefresh(metaDataId) {
    var item = findItemByMetadataId(metaDataId);
    var ui = ensureMetadataUi(item);
    ui.error = '';
    ui.statusMessage = '';
    render();
    requestMetadataSearch(metaDataId).then(function () {
      patchItemMetadata(metaDataId, merge(getCurrentMetadata(item), { status: 'PENDING' }));
      ui.statusMessage = 'Metadata search requested.';
      render();
    }, function (error) {
      ui.error = getErrorMessage(error, 'Failed to request metadata search.');
      render();
    });
  }

  function patchItemMetadata(metaDataId, updatedMetadata) {
    var normalized = normalizeMetadata(updatedMetadata);
    visitAllItems(function (item) {
      var current = getCurrentMetadata(item);
      if (current.metaDataId === metaDataId) {
        item.metaData = clone(normalized);
        item.metadata = clone(normalized);
        if (normalized.title) {
          item.title = normalized.title;
        }
        if (normalized.plotSummary) {
          item.plotSummary = normalized.plotSummary;
          item.description = normalized.plotSummary;
        }
        if (normalized.airDate) {
          item.releaseDate = normalized.airDate;
        }
        if (normalized.rating !== '') {
          item.rating = normalized.rating;
        }
      }
    });

    if (state.movie && getCurrentMetadata(state.movie).metaDataId === metaDataId) {
      state.movie.metaData = clone(normalized);
      state.movie.metadata = clone(normalized);
    }
    if (state.selectedSeries && getCurrentMetadata(state.selectedSeries).metaDataId === metaDataId) {
      state.selectedSeries.metaData = clone(normalized);
      state.selectedSeries.metadata = clone(normalized);
    }
  }

  function visitAllItems(visitor) {
    visitArray(state.series, visitor);
    visitArray(state.movies, visitor);
    visitArray(state.seasons, visitor);
    visitArray(state.episodes, visitor);
    if (state.movie) {
      visitor(state.movie);
    }
    if (state.selectedSeries) {
      visitor(state.selectedSeries);
    }
    if (state.selectedSeason) {
      visitor(state.selectedSeason);
    }
    if (state.activeMedia) {
      visitor(state.activeMedia);
    }
  }

  function visitArray(items, visitor) {
    var i;
    for (i = 0; i < items.length; i += 1) {
      visitor(items[i]);
    }
  }

  function ensureMetadataUi(item) {
    var metadata = normalizeMetadata(item && (item.metaData || item.metadata || {}));
    var key = metadata.metaDataId || ('no-metadata-' + resolveMediaId(item));
    if (!state.metadataUi[key]) {
      state.metadataUi[key] = {
        editing: false,
        saving: false,
        error: '',
        statusMessage: '',
        draft: metadata
      };
    }
    return state.metadataUi[key];
  }

  function getCurrentMetadata(item) {
    return normalizeMetadata(item && (item.metaData || item.metadata || {}), item);
  }

  function normalizeMetadata(metadata, item) {
    var source = metadata || {};
    return {
      metaDataId: source.metaDataId || (item && item.metaDataId) || '',
      title: source.title || (item && item.title) || '',
      plotSummary: source.plotSummary || (item && (item.plotSummary || item.description)) || '',
      airDate: normalizeDateInput(source.airDate || source.releaseDate || (item && item.releaseDate) || ''),
      rating: normalizeRatingInput(source.rating !== undefined ? source.rating : item && item.rating),
      status: source.status || 'PENDING',
      message: source.message || ''
    };
  }

  function normalizeDateInput(value) {
    if (!value) {
      return '';
    }
    var raw = String(value).replace(/^\s+|\s+$/g, '');
    if (!raw) {
      return '';
    }
    if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) {
      return raw;
    }
    var parsed = new Date(raw);
    if (isNaN(parsed.getTime())) {
      return '';
    }
    return parsed.toISOString().slice(0, 10);
  }

  function normalizeRatingInput(value) {
    if (value === null || value === undefined || String(value).replace(/^\s+|\s+$/g, '') === '') {
      return '';
    }
    var parsed = parseFloat(value);
    return isFinite(parsed) ? String(parsed) : '';
  }

  function findItemByMetadataId(metaDataId) {
    var found = null;
    visitAllItems(function (item) {
      if (found) {
        return;
      }
      if (getCurrentMetadata(item).metaDataId === metaDataId) {
        found = item;
      }
    });
    return found;
  }

  function findEpisodeById(mediaId) {
    var i;
    for (i = 0; i < state.episodes.length; i += 1) {
      if (String(resolveMediaId(state.episodes[i])) === String(mediaId)) {
        return state.episodes[i];
      }
    }
    return null;
  }

  function findSearchResultByKey(resultKey) {
    var groups = state.search.resultsBySource;
    var source;
    var items;
    var i;
    for (source in groups) {
      if (groups.hasOwnProperty(source)) {
        items = groups[source] || [];
        for (i = 0; i < items.length; i += 1) {
          if ((items[i].magnetLink || items[i].sourceUrl || items[i].title || '') === resultKey) {
            return items[i];
          }
        }
      }
    }
    return null;
  }

  function buildMovieEntries(item, idLabel) {
    return buildSeriesEntries(item, idLabel);
  }

  function buildSeriesEntries(item, idLabel) {
    var entries = [];
    var metadata = item && (item.metadata || item.metaData || {});
    var key;
    entries.push({ label: idLabel || 'ID', value: displayValue(resolveMediaId(item) || item.seriesId || item.seasonId || item.id) });
    entries.push({ label: 'Type', value: displayValue(item && item.type) });
    entries.push({ label: 'Title', value: displayValue(item && item.title) });
    entries.push({ label: 'Release Date', value: displayValue(formatReleaseDate(item && item.releaseDate) || 'N/A') });
    entries.push({ label: 'Year', value: displayValue(item && item.year) });
    entries.push({ label: 'Rating', value: formatRating(item && item.rating) });
    entries.push({ label: 'Series', value: displayValue(item && item.seriesName) });
    entries.push({ label: 'Season', value: displayValue(item && item.seasonName) });
    entries.push({ label: 'Season #', value: displayValue(item && item.seasonNumber) });
    entries.push({ label: 'Episode #', value: displayValue(item && item.episodeNumber) });
    entries.push({ label: 'Poster URL', value: displayValue(item && item.posterUrl) });
    entries.push({ label: 'Stream URL', value: displayValue(item && item.streamUrl) });
    entries.push({ label: 'Summary', value: displayValue(item && (item.plotSummary || item.description)) });
    for (key in metadata) {
      if (metadata.hasOwnProperty(key)) {
        entries.push({ label: 'meta.' + key, value: displayValue(metadata[key]) });
      }
    }
    return entries;
  }

  function sortResultGroups(resultsBySource, sortBy) {
    var groups = [];
    var source;
    for (source in resultsBySource) {
      if (resultsBySource.hasOwnProperty(source)) {
        groups.push({
          source: source,
          items: resultsBySource[source].slice(0).sort(function (left, right) {
            if (sortBy === 'size') {
              return parseSizeToBytes(right.size) - parseSizeToBytes(left.size);
            }
            return parseSeeders(right.seeders) - parseSeeders(left.seeders);
          })
        });
      }
    }
    groups.sort(function (left, right) {
      return String(left.source).localeCompare(String(right.source));
    });
    return groups;
  }

  function parseSeeders(value) {
    var parsed = parseInt(String(value || '').replace(/[^\d]/g, ''), 10);
    return isFinite(parsed) ? parsed : 0;
  }

  function parseSizeToBytes(size) {
    var match;
    var value;
    var unit;
    var multipliers;
    if (!size) {
      return 0;
    }
    match = String(size).replace(/^\s+|\s+$/g, '').match(/([\d.]+)\s*([kmgtp]?b)/i);
    if (!match) {
      return 0;
    }
    value = parseFloat(match[1]);
    if (!isFinite(value)) {
      return 0;
    }
    unit = String(match[2]).toUpperCase();
    multipliers = { B: 1, KB: 1024, MB: 1024 * 1024, GB: 1024 * 1024 * 1024, TB: 1099511627776, PB: 1125899906842624 };
    return value * (multipliers[unit] || 1);
  }

  function normalizeSeriesItem(item) {
    var metadata = pickMetaBlock(item);
    var releaseDate = metadata.firstAirDate || metadata.releaseDate || item.releaseDate || null;
    var numericRating = parseFloat(metadata.rating !== undefined ? metadata.rating : item.rating);
    return {
      id: item.id || item.seriesId,
      mediaId: item.mediaId || item.seriesId || item.id,
      seriesId: item.seriesId || item.id,
      type: item.type || 'series',
      title: metadata.title || item.title || item.name || 'Series',
      plotSummary: metadata.plotSummary || item.plotSummary || item.description || '',
      description: metadata.plotSummary || item.plotSummary || item.description || '',
      releaseDate: releaseDate,
      year: item.year || (releaseDate ? new Date(releaseDate).getFullYear() : 0),
      rating: isFinite(numericRating) ? numericRating : null,
      posterUrl: metadata.posterUrl || item.posterUrl || (item.poster && item.poster.url) || '',
      streamUrl: item.streamUrl || item.filePath || '',
      metadata: metadata,
      metaData: metadata,
      poster: item.poster || null
    };
  }

  function normalizeSeasonItem(item, parentSeries) {
    var metadata = pickMetaBlock(item);
    return {
      id: item.id || item.seasonId,
      mediaId: item.mediaId || item.seasonId || item.id,
      seasonId: item.seasonId || item.id,
      seriesId: item.seriesId || (parentSeries && parentSeries.seriesId),
      type: 'season',
      title: metadata.title || item.title || item.name || buildSeasonLabel(item),
      plotSummary: metadata.plotSummary || item.plotSummary || item.description || '',
      description: metadata.plotSummary || item.plotSummary || item.description || '',
      seasonName: metadata.title || item.name || buildSeasonLabel(item),
      seasonNumber: item.seasonNumber,
      releaseDate: metadata.releaseDate || metadata.firstAirDate || item.releaseDate || null,
      rating: isFinite(parseFloat(metadata.rating !== undefined ? metadata.rating : item.rating)) ? parseFloat(metadata.rating !== undefined ? metadata.rating : item.rating) : null,
      posterUrl: metadata.posterUrl || item.posterUrl || (parentSeries && parentSeries.posterUrl) || '',
      streamUrl: item.streamUrl || item.filePath || '',
      metadata: metadata,
      metaData: metadata,
      poster: item.poster || null
    };
  }

  function normalizeEpisodeItem(item) {
    var metadata = pickMetaBlock(item);
    var mediaItem = item.media || item.mediaItem || {};
    var releaseDate = metadata.releaseDate || metadata.airDate || metadata.firstAirDate || item.releaseDate || null;
    var numericRating = parseFloat(metadata.rating !== undefined ? metadata.rating : item.rating);
    return {
      mediaId: item.mediaId || mediaItem.mediaId || item.id,
      id: item.id || item.mediaId || mediaItem.mediaId,
      seriesId: item.seriesId || (state.selectedSeason && state.selectedSeason.seriesId) || (state.selectedSeries && state.selectedSeries.seriesId) || null,
      seasonId: item.seasonId || (state.selectedSeason && state.selectedSeason.seasonId) || null,
      type: item.type || 'episode',
      title: metadata.title || item.title || item.name || 'Episode',
      plotSummary: metadata.plotSummary || item.plotSummary || item.description || '',
      description: metadata.plotSummary || item.plotSummary || item.description || '',
      seriesName: item.seriesName || (state.selectedSeries && state.selectedSeries.title) || null,
      seasonName: item.seasonName || (state.selectedSeason && state.selectedSeason.title) || null,
      seasonNumber: item.seasonNumber !== undefined ? item.seasonNumber : (state.selectedSeason && state.selectedSeason.seasonNumber),
      episodeNumber: item.episodeNumber !== undefined ? item.episodeNumber : null,
      releaseDate: releaseDate,
      year: item.year || (releaseDate ? new Date(releaseDate).getFullYear() : null),
      rating: isFinite(numericRating) ? numericRating : null,
      posterUrl: metadata.posterUrl || item.posterUrl || (state.selectedSeries && state.selectedSeries.posterUrl) || '',
      streamUrl: item.streamUrl || item.filePath || mediaItem.filePath || '',
      metadata: metadata,
      metaData: metadata,
      poster: item.poster || null
    };
  }

  function normalizeMovieItem(item) {
    var metadata = pickMetaBlock(item);
    var mediaItem = item.mediaItem || item.media || {};
    var releaseDate = metadata.releaseDate || metadata.airDate || item.releaseDate || null;
    var numericRating = parseFloat(metadata.rating !== undefined ? metadata.rating : item.rating);
    return {
      mediaId: item.mediaId || mediaItem.mediaId || item.movieId || item.id,
      id: item.id || item.mediaId || mediaItem.mediaId,
      movieId: item.movieId || item.id || null,
      type: item.type || 'movie',
      title: metadata.title || item.title || item.name || 'Movie',
      plotSummary: metadata.plotSummary || item.plotSummary || item.description || '',
      description: metadata.plotSummary || item.plotSummary || item.description || '',
      releaseDate: releaseDate,
      year: item.year || (releaseDate ? new Date(releaseDate).getFullYear() : null),
      rating: isFinite(numericRating) ? numericRating : null,
      streamUrl: item.streamUrl || item.filePath || mediaItem.filePath || '',
      posterUrl: metadata.posterUrl || item.posterUrl || normalizePosterImageData(item),
      metadata: metadata,
      metaData: metadata,
      mediaItem: mediaItem,
      poster: item.poster || null
    };
  }

  function normalizePosterImageData(item) {
    var imageData = item && item.poster && item.poster.imageData;
    if (imageData === null || imageData === undefined) {
      return item && item.posterUrl ? item.posterUrl : '';
    }
    if (typeof imageData === 'string') {
      return imageData;
    }
    if (Object.prototype.toString.call(imageData) === '[object Array]') {
      return bytesToBase64(imageData) || (item && item.posterUrl ? item.posterUrl : '');
    }
    return item && item.posterUrl ? item.posterUrl : '';
  }

  function pickMetaBlock(item) {
    if (!item) {
      return {};
    }
    return item.metaData || item.metadata || item.meta || {};
  }

  function buildSeasonLabel(item) {
    return ('Season ' + displayValue(item && item.seasonNumber || '')).replace(/\s+$/, '');
  }

  function formatReleaseDate(value) {
    if (!value) {
      return '';
    }
    var parsed = new Date(value);
    if (isNaN(parsed.getTime())) {
      return String(value);
    }
    return parsed.toLocaleDateString();
  }

  function formatYear(value) {
    if (!value) {
      return '';
    }
    var parsed = new Date(value);
    return isNaN(parsed.getTime()) ? '' : String(parsed.getFullYear());
  }

  function formatRating(value) {
    var parsed = parseFloat(value);
    if (!isFinite(parsed)) {
      return 'N/A';
    }
    return parsed.toFixed(1);
  }

  function displayValue(value) {
    if (value === null || value === undefined || String(value).replace(/^\s+|\s+$/g, '') === '') {
      return 'N/A';
    }
    return String(value);
  }

  function formatBytes(bytes) {
    var value = Number(bytes);
    var units = ['B', 'KB', 'MB', 'GB', 'TB'];
    var unitIndex = 0;
    if (!isFinite(value) || value < 0) {
      return 'N/A';
    }
    while (value >= 1024 && unitIndex < units.length - 1) {
      value = value / 1024;
      unitIndex += 1;
    }
    return value.toFixed(value >= 100 ? 0 : 1) + ' ' + units[unitIndex];
  }

  function formatSpeed(bytesPerSecond) {
    var value = Number(bytesPerSecond);
    if (!isFinite(value) || value < 0) {
      return 'N/A';
    }
    return formatBytes(value) + '/s';
  }

  function formatImportMediaStatus(value) {
    var tokens;
    var i;
    var result = [];
    if (!value) {
      return 'N/A';
    }
    tokens = String(value).split('_');
    for (i = 0; i < tokens.length; i += 1) {
      if (tokens[i]) {
        result.push(tokens[i].charAt(0) + tokens[i].substring(1).toLowerCase());
      }
    }
    return result.join(' ');
  }

  function getCompletionPercent(item) {
    var total = Number(item && item.totalSize);
    var downloaded = Number(item && item.downloadedSize);
    if (!isFinite(total) || total <= 0 || !isFinite(downloaded) || downloaded < 0) {
      return 0;
    }
    return Math.max(0, Math.min(100, (downloaded / total) * 100));
  }

  function getMediaDownloadPercent(mediaInfo) {
    var total = Number(mediaInfo && mediaInfo.fileSize);
    var downloaded = Number(mediaInfo && mediaInfo.bytesDownloaded);
    if (!isFinite(total) || total <= 0 || !isFinite(downloaded) || downloaded < 0) {
      return 0;
    }
    return Math.max(0, Math.min(100, (downloaded / total) * 100));
  }

  function normalizePosterSrc(item) {
    var raw = extractPosterImageData(item) || item.posterUrl || '';
    var base64Payload;
    if (!raw) {
      return '';
    }
    raw = String(raw).replace(/^\s+|\s+$/g, '');
    if (!raw) {
      return '';
    }
    if (/^data:image\//i.test(raw) || /^https?:\/\//i.test(raw) || /^blob:/i.test(raw) || raw.charAt(0) === '/' || raw.indexOf('./') === 0 || raw.indexOf('../') === 0) {
      return raw;
    }
    base64Payload = raw.replace(/\s+/g, '');
    if (/^[A-Za-z0-9+/=]+$/.test(base64Payload) && base64Payload.length >= 32) {
      return 'data:' + detectBase64ImageMime(base64Payload) + ';base64,' + base64Payload;
    }
    return raw;
  }

  function detectBase64ImageMime(base64) {
    if (base64.indexOf('/9j/') === 0) {
      return 'image/jpeg';
    }
    if (base64.indexOf('iVBOR') === 0) {
      return 'image/png';
    }
    if (base64.indexOf('R0lGOD') === 0) {
      return 'image/gif';
    }
    if (base64.indexOf('UklGR') === 0) {
      return 'image/webp';
    }
    return 'image/jpeg';
  }

  function extractPosterImageData(item) {
    var candidate = item && item.poster && item.poster.imageData;
    if (typeof candidate === 'string') {
      return candidate;
    }
    if (Object.prototype.toString.call(candidate) === '[object Array]') {
      return bytesToBase64(candidate);
    }
    if (candidate && Object.prototype.toString.call(candidate.data) === '[object Array]') {
      return bytesToBase64(candidate.data);
    }
    return '';
  }

  function bytesToBase64(bytes) {
    var binary = '';
    var i;
    var chunk;
    var j;
    if (!bytes || !bytes.length) {
      return '';
    }
    for (i = 0; i < bytes.length; i += 8192) {
      chunk = bytes.slice(i, i + 8192);
      for (j = 0; j < chunk.length; j += 1) {
        binary += String.fromCharCode(chunk[j]);
      }
    }
    try {
      return window.btoa(binary);
    } catch (error) {
      return '';
    }
  }

  function resolveMediaId(item) {
    return item && (item.mediaId || item.id || item.seriesId || item.seasonId);
  }

  function capitalize(value) {
    var text = String(value || '');
    if (!text) {
      return '';
    }
    return text.charAt(0).toUpperCase() + text.substring(1).toLowerCase();
  }

  function normalizeArray(value) {
    return Object.prototype.toString.call(value) === '[object Array]' ? value : [];
  }

  function clone(value) {
    return JSON.parse(JSON.stringify(value));
  }

  function merge(target, source) {
    var copy = clone(target || {});
    var key;
    for (key in source) {
      if (source.hasOwnProperty(key)) {
        copy[key] = source[key];
      }
    }
    return copy;
  }

  function getErrorMessage(error, fallback) {
    if (!error) {
      return fallback;
    }
    if (typeof error === 'string') {
      return error;
    }
    return error.message || fallback;
  }

  function apiUrl(path) {
    if (/^https?:\/\//i.test(path)) {
      return path;
    }
    return apiBaseUrl + '/' + String(path).replace(/^\/+/, '');
  }

  function resolveMediaUrl(value) {
    if (!value) {
      return '';
    }
    if (/^https?:\/\//i.test(value) || /^data:/i.test(value) || /^blob:/i.test(value)) {
      return value;
    }
    return apiBaseUrl + '/' + String(value).replace(/^\/+/, '');
  }

  function selectedIf(currentValue, expectedValue) {
    return String(currentValue) === String(expectedValue) ? ' selected="selected"' : '';
  }

  function getInputValue(id) {
    var element = document.getElementById(id);
    return element ? element.value : '';
  }

  function navigateToPage(fileName, queryParams) {
    window.location.href = fileName + buildQueryString(queryParams || {});
  }

  function replaceCurrentQuery(queryParams) {
    if (!window.history || !window.history.replaceState) {
      return;
    }

    window.history.replaceState(null, document.title, buildCurrentFileName() + buildQueryString(queryParams || {}));
  }

  function buildCurrentFileName() {
    var path = window.location.pathname || '';
    return path.split('/').pop() || 'index.html';
  }

  function getFormValue(form, name) {
    var field = form.elements[name];
    return field ? field.value : '';
  }

  function escapeHtml(value) {
    return String(value === null || value === undefined ? '' : value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function escapeAttribute(value) {
    return escapeHtml(value).replace(/`/g, '&#96;');
  }

  function request(options) {
    return new Promise(function (resolve, reject) {
      var xhr = new XMLHttpRequest();
      var headers = options.headers || {};
      var body = options.body;
      var hasFormDataBody = typeof window.FormData !== 'undefined' && body instanceof window.FormData;
      xhr.open(options.method || 'GET', options.url, true);
      xhr.setRequestHeader('X-API-Key', apiKey);
      if (!hasFormDataBody) {
        xhr.setRequestHeader('Content-Type', headers['Content-Type'] || 'application/json');
      }
      setHeaders(xhr, headers);
      xhr.onreadystatechange = function () {
        var responseText;
        var contentType;
        if (xhr.readyState !== 4) {
          return;
        }
        responseText = xhr.responseText || '';
        if (xhr.status < 200 || xhr.status >= 300) {
          reject(new Error(responseText || ('Request failed: ' + xhr.status)));
          return;
        }
        contentType = xhr.getResponseHeader('content-type') || '';
        if (contentType.indexOf('application/json') !== -1) {
          try {
            resolve(responseText ? JSON.parse(responseText) : null);
          } catch (error) {
            reject(error);
          }
          return;
        }
        resolve(responseText);
      };
      xhr.onerror = function () {
        reject(new Error('Network request failed.'));
      };
      xhr.send(hasFormDataBody ? body : (body || null));
    });
  }

  function setHeaders(xhr, headers) {
    var key;
    for (key in headers) {
      if (headers.hasOwnProperty(key) && key !== 'Content-Type') {
        xhr.setRequestHeader(key, headers[key]);
      }
    }
  }

  function openSseRequest(url, handlers) {
    var xhr = new XMLHttpRequest();
    var closed = false;
    var responseIndex = 0;
    var buffer = '';
    var sawSuccess = false;

    xhr.open('GET', url, true);
    xhr.setRequestHeader('Accept', 'text/event-stream');
    xhr.setRequestHeader('X-API-Key', apiKey);

    xhr.onreadystatechange = function () {
      if (closed) {
        return;
      }
      if (xhr.readyState === 2 && (xhr.status < 200 || xhr.status >= 300)) {
        closed = true;
        if (handlers.onError) {
          handlers.onError(xhr.responseText || ('Request failed: ' + xhr.status));
        }
        xhr.abort();
        return;
      }
      if (xhr.readyState === 3 || xhr.readyState === 4) {
        sawSuccess = true;
        consumeSseText(xhr.responseText || '');
      }
      if (xhr.readyState === 4 && !closed && handlers.onDone) {
        handlers.onDone();
      }
    };

    xhr.onerror = function () {
      if (!closed && handlers.onError) {
        handlers.onError('Streaming request failed.');
      }
    };

    xhr.send(null);

    return {
      abort: function () {
        closed = true;
        try {
          xhr.abort();
        } catch (error) {
        }
      }
    };

    function consumeSseText(fullText) {
      var nextChunk;
      var normalized;
      var events;
      var i;
      if (fullText.length < responseIndex) {
        responseIndex = 0;
        buffer = '';
      }
      nextChunk = fullText.substring(responseIndex);
      responseIndex = fullText.length;
      buffer += nextChunk;
      normalized = buffer.replace(/\r\n/g, '\n');
      events = normalized.split('\n\n');
      buffer = events.pop() || '';
      for (i = 0; i < events.length; i += 1) {
        parseSseEvent(events[i]);
      }
    }

    function parseSseEvent(chunk) {
      var lines = chunk.split('\n');
      var eventName = 'message';
      var dataLines = [];
      var i;
      var line;
      var rawData;
      var data;
      for (i = 0; i < lines.length; i += 1) {
        line = String(lines[i] || '').replace(/\s+$/, '');
        if (!line || line.charAt(0) === ':') {
          continue;
        }
        if (line.indexOf('event:') === 0) {
          eventName = line.substring(6).replace(/^\s+/, '') || 'message';
          continue;
        }
        if (line.indexOf('data:') === 0) {
          dataLines.push(line.substring(5).replace(/^\s+/, ''));
        }
      }
      if (!dataLines.length) {
        return;
      }
      rawData = dataLines.join('\n');
      data = rawData;
      try {
        data = JSON.parse(rawData);
      } catch (error) {
      }
      if (eventName === 'error') {
        if (handlers.onError) {
          handlers.onError(data);
        }
        return;
      }
      if (handlers.onUpdate) {
        handlers.onUpdate(data);
      }
      if (handlers.onItem) {
        handlers.onItem(data);
      }
    }
  }

  function searchCatalogSeries(query) {
    return request({ url: apiUrl('/api/media/series' + buildQueryString({ query: query })) });
  }

  function searchCatalogSeasons(seriesId, query) {
    return request({ url: apiUrl('/api/media/seasons' + buildQueryString({ seriesId: seriesId, query: query })) });
  }

  function searchCatalogEpisodes(seriesId, seasonId, query) {
    return request({ url: apiUrl('/api/media/episodes' + buildQueryString({ seriesId: seriesId, seasonId: seasonId, query: query })) });
  }

  function searchCatalogMovies(query) {
    return request({ url: apiUrl('/api/media/movies' + buildQueryString({ query: query })) });
  }

  function searchAcquisitionStream(query, category, handlers) {
    return openSseRequest(apiUrl('/api/acquisition/search' + buildQueryString({ query: query, category: category })), {
      onItem: handlers.onItem,
      onError: handlers.onError,
      onDone: handlers.onDone
    });
  }

  function uploadMediaFile(file, payload) {
    var formData = new window.FormData();
    var key;
    formData.append('file', file);
    for (key in payload) {
      if (payload.hasOwnProperty(key) && payload[key] !== null && payload[key] !== undefined && payload[key] !== '') {
        formData.append(key, payload[key]);
      }
    }
    return request({
      url: apiUrl('/api/stream/upload'),
      method: 'POST',
      body: formData
    });
  }

  function importStreamMedia(payload) {
    return request({
      url: apiUrl('/api/stream/importRequest'),
      method: 'POST',
      body: JSON.stringify(payload)
    });
  }

  function getTorrentInfo() {
    return request({ url: apiUrl('/api/stream/torrent/info') });
  }

  function pauseTorrent(infoHash) {
    return request({ url: apiUrl('/api/stream/torrent/pause/' + encodeURIComponent(infoHash)), method: 'POST' });
  }

  function resumeTorrent(infoHash) {
    return request({ url: apiUrl('/api/stream/torrent/resume/' + encodeURIComponent(infoHash)), method: 'POST' });
  }

  function deleteTorrent(infoHash) {
    return request({ url: apiUrl('/api/stream/torrent/delete/' + encodeURIComponent(infoHash)), method: 'POST' });
  }

  function updateMetadata(metaDataId, payload) {
    return request({
      url: apiUrl('/api/metadata/update/' + encodeURIComponent(metaDataId)),
      method: 'POST',
      body: JSON.stringify(payload)
    });
  }

  function requestMetadataSearch(metaDataId) {
    return request({
      url: apiUrl('/api/metadata/requestSearch/' + encodeURIComponent(metaDataId)),
      method: 'POST'
    });
  }

  function streamManifest(mediaId, playbackUrl) {
    return request({ url: apiUrl('/api/stream/' + encodeURIComponent(mediaId) + '/manifest' + buildQueryString({ playbackUrl: playbackUrl })) });
  }

  function streamCaptionsUrl(mediaId, lang) {
    return apiUrl('/api/stream/' + encodeURIComponent(mediaId) + '/captions?lang=' + encodeURIComponent(lang || 'en') + '&api_key=' + encodeURIComponent(apiKey));
  }

  function buildQueryString(params) {
    var query = [];
    var key;
    for (key in params) {
      if (params.hasOwnProperty(key) && params[key] !== null && params[key] !== undefined && String(params[key]).replace(/^\s+|\s+$/g, '') !== '') {
        query.push(encodeURIComponent(key) + '=' + encodeURIComponent(params[key]));
      }
    }
    return query.length ? '?' + query.join('&') : '';
  }
}());