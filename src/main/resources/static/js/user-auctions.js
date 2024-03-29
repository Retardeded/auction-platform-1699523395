function handleRemoveAuction(event, auctionId) {
  event.preventDefault();

  if (!confirm('Are you sure you want to remove this auction?')) {
    return;
    }

  const csrfTokenMetaTag = document.querySelector('meta[name="_csrf"]');
  const csrfToken = csrfTokenMetaTag ? csrfTokenMetaTag.getAttribute('content') : null;
  const csrfHeaderMetaTag = document.querySelector('meta[name="_csrf_header"]');
  const csrfHeader = csrfHeaderMetaTag ? csrfHeaderMetaTag.getAttribute('content') : null;

  fetch('/auctions/delete/' + auctionId, {
    method: 'DELETE',
    headers: {
            [csrfHeader]: csrfToken
          }
  })
  .then(response => {
    if (response.ok) {
    } else if (response.status === 403) {
      alert('You cannot delete this auction.');
    } else if (response.status === 404) {
      alert('Auction not found.');
    } else {
      alert('An error occurred. Please try again.');
    }
    const auctionItem = document.getElementById('auction-item-' + auctionId);
    if (auctionItem) {
      auctionItem.remove();
    }
  })
  .catch(error => {
    console.error('Error:', error);
    alert('Could not remove the auction. Maybe there are now bids on it.');
  });
}

function handleEditAuction(event, auctionId) {
  event.preventDefault();
  const auctionSlug = event.currentTarget.getAttribute('data-auction-slug');
  window.location.href = `/auction/${auctionSlug}/edit`;
}