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

function submitFeatureAuction() {
    var form = document.getElementById('featureAuctionForm');
    var formData = new FormData(form);
    fetch(form.getAttribute('action'), {
        method: 'POST',
        body: formData,
        headers: {
            'X-CSRF-TOKEN': formData.get('_csrf')
        }
    }).then(response => response.json())
    .then(data => {
        if (data.imagePath) {
            var auctionId = formData.get('auctionId');
            var featuredLogoContainer = document.getElementById(`featured-logo-${auctionId}`);
            if (featuredLogoContainer) {
                featuredLogoContainer.innerHTML = `<img src="${data.imagePath}" alt="Featured Auction Label" style="display:block;">`;
            }
            closeEditModal();
        } else {
            console.error('Failed to update feature type');
        }
    }).catch(error => {
        console.error('Error:', error);
    });
}