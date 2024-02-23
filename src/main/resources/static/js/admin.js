function showEditModal(button) {
    var auctionId = button.getAttribute('data-auction-id');
    var featuredType = button.getAttribute('data-featured-type');

    document.getElementById('modalAuctionId').value = auctionId;

    var selectElement = document.getElementById('modalFeaturedType');
    selectElement.value = featuredType;

    document.getElementById('editModal').style.display = 'block';
}

function closeEditModal() {
    document.getElementById('editModal').style.display = 'none';
}

window.onclick = function(event) {
    if (event.target == document.getElementById('editModal')) {
        closeEditModal();
    }
}

function submitForm(form) {
    form.submit();
}