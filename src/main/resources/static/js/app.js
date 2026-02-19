$(document).ready(function() {
    // 1. Initialize the DataTable (US-05 & US-06)
    const table = $('#gamesTable').DataTable({
        ajax: {
            url: '/api/games',
            dataSrc: ''
        },
        columns: [
            { data: 'title' },
            { data: 'genre' },
            { data: 'status' },
            { data: 'launcherName' },
            {
                data: 'purchasePrice',
                render: function(data) {
                    if (data === null || data === undefined) return '';
                    return '£' + Number(data).toFixed(2);
                }
            }
        ]
    });

    // 2. Handle the CSV Upload (US-03 & US-04)
    $('#uploadForm').on('submit', function(e) {
        e.preventDefault();

        let formData = new FormData();
        formData.append('file', $('#gameFile')[0].files[0]);

        $('#uploadStatus').html('<div class="text-info">Uploading...</div>');

        $.ajax({
            url: '/api/games/upload',
            type: 'POST',
            data: formData,
            processData: false,
            contentType: false,
            success: function(response) {
                // US-04: Clear notification on successd
                $('#uploadStatus').html('<div class="alert alert-success">' + response + '</div>');
                table.ajax.reload(); // Refresh the table to show new games
            },
            error: function(xhr) {
                // US-04: Clear notification on error
                $('#uploadStatus').html('<div class="alert alert-danger">' + xhr.responseText + '</div>');
            }
        });
    });
});