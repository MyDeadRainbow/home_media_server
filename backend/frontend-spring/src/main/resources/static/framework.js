const apiBaseUrl = 'http://localhost:8085';

function apiUrl(path) {
    if (/^https?:\/\//i.test(path)) {
        return path;
    }
    return apiBaseUrl + '/' + String(path).replace(/^\/+/, '');
}

(function installFormDataCompat(globalScope) {
    var NativeFormData = globalScope && globalScope.FormData;
    var nativePrototype = NativeFormData && NativeFormData.prototype;
    var nativeAppend = nativePrototype && nativePrototype.append;
    var needsCompat = false;
    var methodNames;
    var i;

    if (!NativeFormData || !nativePrototype || typeof nativeAppend !== 'function') {
        return;
    }

    methodNames = ['delete', 'get', 'getAll', 'has', 'set', 'entries', 'keys', 'values', 'forEach'];
    for (i = 0; i < methodNames.length; i += 1) {
        if (typeof nativePrototype[methodNames[i]] !== 'function') {
            needsCompat = true;
            break;
        }
    }
    if (!needsCompat) {
        return;
    }

    var DATA_KEY = '__hmsFormDataEntries__';
    var SEND_KEY = '__hmsFormDataNeedsSync__';
    var nativeDelete = nativePrototype['delete'];
    var nativeSet = nativePrototype.set;
    var nativeGet = nativePrototype.get;
    var nativeGetAll = nativePrototype.getAll;
    var nativeHas = nativePrototype.has;
    var nativeEntries = nativePrototype.entries;
    var nativeKeys = nativePrototype.keys;
    var nativeValues = nativePrototype.values;
    var nativeForEach = nativePrototype.forEach;
    var nativeSend = globalScope.XMLHttpRequest && globalScope.XMLHttpRequest.prototype && globalScope.XMLHttpRequest.prototype.send;

    function canUseSymbolIterator() {
        return typeof globalScope.Symbol === 'function' && !!globalScope.Symbol.iterator;
    }

    function createIterator(list) {
        var index = 0;
        var iterator = {
            next: function () {
                if (index >= list.length) {
                    return { value: undefined, done: true };
                }
                var value = list[index];
                index += 1;
                return { value: value, done: false };
            }
        };

        if (canUseSymbolIterator()) {
            iterator[globalScope.Symbol.iterator] = function () {
                return iterator;
            };
        }

        return iterator;
    }

    function isFileInput(element) {
        return element && element.tagName === 'INPUT' && String(element.type || '').toLowerCase() === 'file';
    }

    function collectPairsFromForm(form) {
        var pairs = [];
        var elements;
        var index;

        if (!form || !form.elements) {
            return pairs;
        }

        elements = form.elements;
        for (index = 0; index < elements.length; index += 1) {
            var field = elements[index];
            var tagName = field && field.tagName;
            var type = String((field && field.type) || '').toLowerCase();
            var name = field && field.name;

            if (!name || field.disabled) {
                continue;
            }

            if (type === 'submit' || type === 'button' || type === 'reset' || type === 'image') {
                continue;
            }

            if ((type === 'checkbox' || type === 'radio') && !field.checked) {
                continue;
            }

            if (isFileInput(field)) {
                if (field.files && field.files.length) {
                    var fileIndex;
                    for (fileIndex = 0; fileIndex < field.files.length; fileIndex += 1) {
                        pairs.push({ name: String(name), value: field.files[fileIndex] });
                    }
                }
                continue;
            }

            if (tagName === 'SELECT' && field.multiple) {
                var optionIndex;
                for (optionIndex = 0; optionIndex < field.options.length; optionIndex += 1) {
                    if (field.options[optionIndex].selected) {
                        pairs.push({ name: String(name), value: String(field.options[optionIndex].value) });
                    }
                }
                continue;
            }

            pairs.push({ name: String(name), value: String(field.value) });
        }

        return pairs;
    }

    function ensureStore(formData) {
        if (!formData[DATA_KEY]) {
            Object.defineProperty(formData, DATA_KEY, {
                value: [],
                writable: true,
                configurable: true
            });
        }
        return formData[DATA_KEY];
    }

    function markNeedsSync(formData) {
        Object.defineProperty(formData, SEND_KEY, {
            value: true,
            writable: true,
            configurable: true
        });
    }

    function cloneEntry(entry) {
        return {
            name: entry.name,
            value: entry.value,
            filename: entry.filename
        };
    }

    function buildNativeFromStore(formData) {
        var store = ensureStore(formData);
        var rebuilt = new NativeFormData();
        var idx;
        for (idx = 0; idx < store.length; idx += 1) {
            if (typeof store[idx].filename !== 'undefined') {
                rebuilt.append(store[idx].name, store[idx].value, store[idx].filename);
            } else {
                rebuilt.append(store[idx].name, store[idx].value);
            }
        }
        return rebuilt;
    }

    function WrappedFormData(form) {
        var instance;
        var initialPairs;
        var store;

        if (arguments.length > 0) {
            instance = new NativeFormData(form);
        } else {
            instance = new NativeFormData();
        }

        store = ensureStore(instance);
        if (arguments.length > 0 && form && form.tagName === 'FORM') {
            initialPairs = collectPairsFromForm(form);
            for (i = 0; i < initialPairs.length; i += 1) {
                store.push(initialPairs[i]);
            }
            markNeedsSync(instance);
        }

        return instance;
    }

    WrappedFormData.prototype = nativePrototype;
    globalScope.FormData = WrappedFormData;

    nativePrototype.append = function (name, value, filename) {
        var store = ensureStore(this);
        var entry = {
            name: String(name),
            value: value
        };
        if (arguments.length >= 3) {
            entry.filename = filename;
        }
        store.push(entry);
        markNeedsSync(this);
        if (typeof filename !== 'undefined') {
            return nativeAppend.call(this, name, value, filename);
        }
        return nativeAppend.call(this, name, value);
    };

    nativePrototype['delete'] = function (name) {
        var normalizedName = String(name);
        var store = ensureStore(this);
        var kept = [];
        var idx;

        for (idx = 0; idx < store.length; idx += 1) {
            if (store[idx].name !== normalizedName) {
                kept.push(cloneEntry(store[idx]));
            }
        }

        this[DATA_KEY] = kept;
        markNeedsSync(this);
        if (typeof nativeDelete === 'function') {
            nativeDelete.call(this, normalizedName);
        }
    };

    nativePrototype.get = function (name) {
        var normalizedName = String(name);
        var store = ensureStore(this);
        var idx;

        if (typeof nativeGet === 'function') {
            return nativeGet.call(this, normalizedName);
        }

        for (idx = 0; idx < store.length; idx += 1) {
            if (store[idx].name === normalizedName) {
                return store[idx].value;
            }
        }

        return null;
    };

    nativePrototype.getAll = function (name) {
        var normalizedName = String(name);
        var store = ensureStore(this);
        var values = [];
        var idx;

        if (typeof nativeGetAll === 'function') {
            return nativeGetAll.call(this, normalizedName);
        }

        for (idx = 0; idx < store.length; idx += 1) {
            if (store[idx].name === normalizedName) {
                values.push(store[idx].value);
            }
        }

        return values;
    };

    nativePrototype.has = function (name) {
        var normalizedName = String(name);
        var store = ensureStore(this);
        var idx;

        if (typeof nativeHas === 'function') {
            return nativeHas.call(this, normalizedName);
        }

        for (idx = 0; idx < store.length; idx += 1) {
            if (store[idx].name === normalizedName) {
                return true;
            }
        }

        return false;
    };

    nativePrototype.set = function (name, value, filename) {
        var normalizedName = String(name);
        var store = ensureStore(this);
        var replaced = false;
        var rebuilt = [];
        var idx;

        if (typeof nativeSet === 'function') {
            if (arguments.length >= 3) {
                nativeSet.call(this, normalizedName, value, filename);
            } else {
                nativeSet.call(this, normalizedName, value);
            }
        }

        for (idx = 0; idx < store.length; idx += 1) {
            if (store[idx].name !== normalizedName) {
                rebuilt.push(cloneEntry(store[idx]));
                continue;
            }
            if (!replaced) {
                rebuilt.push({
                    name: normalizedName,
                    value: value,
                    filename: arguments.length >= 3 ? filename : undefined
                });
                replaced = true;
            }
        }

        if (!replaced) {
            rebuilt.push({
                name: normalizedName,
                value: value,
                filename: arguments.length >= 3 ? filename : undefined
            });
        }

        this[DATA_KEY] = rebuilt;
        markNeedsSync(this);
    };

    nativePrototype.entries = function () {
        var store = ensureStore(this);
        var list = [];
        var idx;

        if (typeof nativeEntries === 'function') {
            return nativeEntries.call(this);
        }

        for (idx = 0; idx < store.length; idx += 1) {
            list.push([store[idx].name, store[idx].value]);
        }

        return createIterator(list);
    };

    nativePrototype.keys = function () {
        var store = ensureStore(this);
        var list = [];
        var idx;

        if (typeof nativeKeys === 'function') {
            return nativeKeys.call(this);
        }

        for (idx = 0; idx < store.length; idx += 1) {
            list.push(store[idx].name);
        }

        return createIterator(list);
    };

    nativePrototype.values = function () {
        var store = ensureStore(this);
        var list = [];
        var idx;

        if (typeof nativeValues === 'function') {
            return nativeValues.call(this);
        }

        for (idx = 0; idx < store.length; idx += 1) {
            list.push(store[idx].value);
        }

        return createIterator(list);
    };

    nativePrototype.forEach = function (callback, thisArg) {
        var store = ensureStore(this);
        var idx;

        if (typeof nativeForEach === 'function') {
            return nativeForEach.call(this, callback, thisArg);
        }

        for (idx = 0; idx < store.length; idx += 1) {
            callback.call(thisArg, store[idx].value, store[idx].name, this);
        }
    };

    if (canUseSymbolIterator() && typeof nativePrototype[globalScope.Symbol.iterator] !== 'function') {
        nativePrototype[globalScope.Symbol.iterator] = nativePrototype.entries;
    }

    if (typeof nativeSend === 'function') {
        globalScope.XMLHttpRequest.prototype.send = function (body) {
            var payload = body;
            if (payload instanceof NativeFormData && payload[SEND_KEY]) {
                payload = buildNativeFromStore(payload);
                payload[SEND_KEY] = false;
            }
            return nativeSend.call(this, payload);
        };
    }
})(window);

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

function openSseRequest(url, handlers) {
    var xhr = new XMLHttpRequest();
    var closed = false;
    var responseIndex = 0;
    var buffer = '';
    var sawSuccess = false;

    xhr.open('GET', url, true);
    xhr.setRequestHeader('Accept', 'text/event-stream');
    // xhr.setRequestHeader('X-API-Key', apiKey);

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

function searchAcquisitionStream(query, category, handlers) {
    return openSseRequest(apiUrl('/api/acquisition/search' + buildQueryString({ query: query, category: category })), {
        onItem: handlers.onItem,
        onError: handlers.onError,
        onDone: handlers.onDone
    });
}

function responseToOutput(event) {
    event.preventDefault();
    var form = event.target;
    var output = form.querySelector('output');
    if (output) {
        output.textContent = '';
    }

    // 2. Initialize the standard XHR object
    const xhr = new XMLHttpRequest();

    // 3. Configure the request using the form's native attributes
    xhr.open(form.method, form.action, true);

    // 4. Set up the event listener to catch the server response
    xhr.onload = function () {
        if (xhr.status >= 200 && xhr.status < 300) {
            // Success: Display server response text directly on the page
            output.style.color = "green";
            output.textContent = 'Success: ' + xhr.responseText;
            form.reset(); // Optional: Clear the form fields
        } else {
            // Server-side Error (e.g., 404, 500)
            output.style.color = "red";
            output.textContent = 'Server Error (' + xhr.status + '): ' + xhr.responseText;
        }
    };

    // 5. Handle network or connectivity errors
    xhr.onerror = function () {
        output.style.color = "red";
        output.textContent = "Network error occurred. Connection failed.";
    };

    // 6. Automatically pack all input fields and send the request
    const formData = new FormData(form);
    xhr.send(formData);
    return false; // Prevent the default form submission
}

function htmxRequest(action, method, formData, target, onNoContent) {
    var xhr = new XMLHttpRequest();
    if (method && method.toUpperCase() === 'GET') {
        action += '?';
        var entries = formData ? formData.entries() : [];
        var queryParams = [];
        // for (var i = 0; i < entries.length; i++) {
        //     var ent = entries[i];
        //     queryParams.push(encodeURIComponent(ent[0]) + '=' + encodeURIComponent(ent[1]));
        // }
        for (var ent of entries) {
            queryParams.push(encodeURIComponent(ent[0]) + '=' + encodeURIComponent(ent[1]));
        }
        action += queryParams.join('&');
        formData = null; // GET requests should not have a body
    }
    xhr.open(method || 'POST', action, true);
    xhr.onload = function () {
        if (xhr.status >= 200 && xhr.status < 300) {
            if (xhr.status === 204 && onNoContent) {
                onNoContent();
            } else if (target) {
                target.innerHTML = xhr.responseText;
            }
        } else {
            if (target) {
                target.innerHTML = 'Server Error (' + xhr.status + '): ' + xhr.responseText;
            }
        }
    };
    xhr.onerror = function () {
        if (target) {
            target.innerHTML = "Network error occurred. Connection failed.";
        }
    };
    xhr.send(formData);
}

function runPoll(pollElement, action, method, formData, target) {
    var interval = parseInt(pollElement.getAttribute('interval'), 10) || 1000;
    var id = setInterval(function () {
        var action = pollElement.getAttribute('action') || '';
        var method = pollElement.getAttribute('method') || 'POST';
        var targetId = pollElement.getAttribute('target') || '';
        var target = targetId ? pollElement.querySelector('[rid=' + targetId + ']') : null;
        var form = pollElement.querySelector('form');
        var formData = form ? new FormData(form) : null;
        if (formData.values().next().value) {
            htmxRequest(action, method, formData, target, function () {
                clearInterval(id); // Stop polling if no content is returned
            });
        } else {
            clearInterval(id); // Stop polling if no form data is available
        }
    }, interval); // Default polling interval, can be parameterized if needed
}

document.addEventListener('DOMContentLoaded', function () {
    const pollElements = document.querySelectorAll('htmx\\:poll');
    for (var i = 0; i < pollElements.length; i++) {
        var pollElement = pollElements[i];
        var interval = parseInt(pollElement.getAttribute('interval'), 10) || 1000;
        var action = pollElement.getAttribute('action') || '';
        var method = pollElement.getAttribute('method') || 'POST';
        var targetId = pollElement.getAttribute('target') || '';
        var target = targetId ? pollElement.querySelector('[rid=' + targetId + ']') : null;
        var form = pollElement.querySelector('form');
        var formData = form ? new FormData(form) : null;
        if (formData.values().next().value) {
            runPoll(pollElement, action, method, formData, target);
        }
    }
    // for (const pollElement of document.querySelectorAll('htmx\\:poll')) {
    //     const interval = parseInt(pollElement.getAttribute('interval'), 10) || 1000;
    //     const action = pollElement.getAttribute('action') || '';
    //     const method = pollElement.getAttribute('method') || 'POST';
    //     const targetId = pollElement.getAttribute('target') || '';
    //     const target = targetId ? pollElement.querySelector('[rid=' + targetId + ']') : null;
    //     const form = pollElement.querySelector('form');
    //     const formData = form ? new FormData(form) : null;
    //     if (formData.values().next().value) {
    //         setInterval(function () {
    //             // const form = pollElement.querySelector('form');
    //             // const formData = form ? new FormData(form) : null;
    //             htmxRequest(action, method, formData, target);
    //         }, interval);
    //     }
    // }
});

