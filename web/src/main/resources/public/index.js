/** @type { HTMLInputElement } */// @ts-ignore
const extractButton = document.getElementById('extractButton');

/** @type { HTMLInputElement } */// @ts-ignore
const inputFilePicker = document.getElementById('inputFilePicker');

/** @type { HTMLDivElement } */// @ts-ignore
const statusLabel = document.getElementById('statusLabel');


inputFilePicker.addEventListener('change', () => extractButton.disabled = inputFilePicker.files.length === 0);
extractButton.addEventListener('click', async () => {
    const selectedFiles = inputFilePicker.files;

    if(selectedFiles.length === 1) {
        statusLabel.innerText = 'Uploading...';

        const selectedFile = selectedFiles[0];
        const params = {
            pageNamingStrategy: document.getElementById('pageNamingStrategySelector').value,
            autosizeColumns: document.getElementById('autosizeColumnsCheckbox').checked,
            emptyColumnSkipMethod: getSelectedRadioButtonGroupValue('emptyColumnSkipMethodGroup'),
            emptyRowSkipMethod: getSelectedRadioButtonGroupValue('emptyRowSkipMethodGroup'),
            pageRowNumberFilterMethod: document.getElementById('pageRowNumberFilterMethod').value,
            pageRowNumberFilterValue: document.getElementById('pageRowNumberFilterValue').value,
            pageColumnNumberFilterMethod: document.getElementById('pageColumnNumberFilterMethod').value,
            pageColumnNumberFilterValue: document.getElementById('pageColumnNumberFilterValue').value
        };

        const response = await fetch(`/extract?${new URLSearchParams(params)}`, { method: 'POST', body: selectedFile });

        const excelDataURL = URL.createObjectURL(await response.blob());
        const downloadTag = document.createElement('a');
        downloadTag.href = excelDataURL;
        downloadTag.download = selectedFile.name.replace('.pdf', '.xlsx');
        downloadTag.click();
        URL.revokeObjectURL(excelDataURL);

        statusLabel.innerText = 'Done!';
    }
});

/** @param { string } name */
function getSelectedRadioButtonGroupValue(name) {
    /** @type { HTMLInputElement | undefined } */
    const selectedButton = Array.prototype.find.call(document.getElementsByName(name), k => k.checked);

    return selectedButton === undefined ? null : selectedButton.value;
}

export {};