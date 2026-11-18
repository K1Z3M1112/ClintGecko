(function () {
    'use strict';

    // Strips the effects that make heavy pages stutter on weak GPUs (blur, shadows, blend modes,
    // parallax, long animations) and lazy-loads images/frames so they only download when needed.
    var CSS = [
        '*,*::before,*::after{',
        'backdrop-filter:none!important;-webkit-backdrop-filter:none!important;',
        'text-shadow:none!important;box-shadow:none!important;',
        'mix-blend-mode:normal!important;will-change:auto!important;',
        'background-attachment:scroll!important;',
        'animation-duration:.001ms!important;animation-delay:0s!important;',
        'animation-iteration-count:1!important;',
        'transition-duration:0s!important;transition-delay:0s!important;',
        'scroll-behavior:auto!important}',
        '*:not(html):not(body):not(img):not(video):not(canvas):not(svg):not(picture){filter:none!important}'
    ].join('');

    function addStyle() {
        try {
            var root = document.documentElement || document.head;
            if (!root || document.getElementById('clint-ds-effects')) return !!root;
            var style = document.createElement('style');
            style.id = 'clint-ds-effects';
            style.textContent = CSS;
            root.appendChild(style);
            return true;
        } catch (e) {
            return false;
        }
    }

    if (!addStyle()) {
        var tries = 0;
        var timer = setInterval(function () {
            if (addStyle() || ++tries > 50) clearInterval(timer);
        }, 20);
    }

    function lazy(el) {
        try {
            if (el.hasAttribute('loading')) return;
            if (el.tagName === 'IMG') {
                el.setAttribute('loading', 'lazy');
                el.setAttribute('decoding', 'async');
            } else if (el.tagName === 'IFRAME') {
                el.setAttribute('loading', 'lazy');
            }
        } catch (e) {}
    }

    function lazyTree(node) {
        if (!node || node.nodeType !== 1) return;
        lazy(node);
        if (!node.querySelectorAll) return;
        var list = node.querySelectorAll('img,iframe');
        for (var i = 0; i < list.length; i++) lazy(list[i]);
    }

    try {
        new MutationObserver(function (records) {
            for (var i = 0; i < records.length; i++) {
                var added = records[i].addedNodes;
                for (var j = 0; j < added.length; j++) lazyTree(added[j]);
            }
        }).observe(document, { childList: true, subtree: true });
        lazyTree(document.documentElement);
    } catch (e) {}
})();
