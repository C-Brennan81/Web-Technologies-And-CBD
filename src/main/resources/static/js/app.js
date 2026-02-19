$(document).ready(function() {
    // Initialize DataTable for US-05 & US-06 [cite: 137, 140]
    const table = $('#gamesTable').DataTable({
        ajax: {
            url: '/api/games',
            dataSrc: ''
        },
        columns: [
            { data: 'title' },
            { data: 'genre' },
            { data: 'launcherName' }, // From our DTO [cite: 22]
            { data: 'status' },
            {
                data: 'purchasePrice',
                render: function(data) { return '€' + data.toFixed(2); }
            },
            {
                // Action button for US-07 (Modals) [cite: 151, 153]
                data: null,
                defaultContent: '<button class="btn btn-info btn-sm view-btn">View</button>'
            }
        ]
    });

    // Handle the "View" button click for Modals [cite: 35, 152]
    $('#gamesTable').on('click', '.view-btn', function() {
        const data = table.row($(this).parents('tr')).data();
        alert('Details for: ' + data.title + '\nStatus: ' + data.status);
        // In the next step, we will replace this alert with a real Bootstrap Modal
    });
});