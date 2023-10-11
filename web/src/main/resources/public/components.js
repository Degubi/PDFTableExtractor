customElements.define('skip-method-selector', class extends HTMLElement {

    constructor() {
        super();
    }

    connectedCallback() {
        const groupName = this.getAttribute('group-name');

        this.innerHTML = `<input id = "none${groupName}" name = "${groupName}" type = "radio" value = "0" checked>` +
                         `<label for = "none${groupName}">None</label>` +
                         `<input id = "leading${groupName}" name = "${groupName}" type = "radio" value = "1">` +
                         `<label for = "leading${groupName}">Leading</label>` +
                         `<input id = "trailing${groupName}" name = "${groupName}" type = "radio" value = "2">` +
                         `<label for = "trailing${groupName}">Trailing</label>` +
                         `<input id = "both${groupName}" name = "${groupName}" type = "radio" value = "2">` +
                         `<label for = "both${groupName}">Both</label>`;
    }
});

customElements.define('page-filter-selector', class extends HTMLElement {

    constructor() {
        super();
    }

    connectedCallback() {
        const selectorName = this.getAttribute('selector-name');
        const valueSelectors = [];

        for(let i = 1; i <= 10; ++i) {
            valueSelectors.push(`<option value = "${i}">${i}</option>`);
        }

        this.innerHTML = `<select id = "${selectorName}Method">` +
                             '<option value = "0">less/equal</option>' +
                             '<option value = "1">equal</option>' +
                             '<option value = "2">greater/equal</option>' +
                         '</select>' +
                         '<span>than/to</span>' +
                         `<select id = "${selectorName}Value">` + valueSelectors.join('') + '</select>';
    }
});