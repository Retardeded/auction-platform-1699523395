document.addEventListener('DOMContentLoaded', (event) => {
  document.querySelectorAll('.remove-watchlist-form').forEach(form => {
    form.addEventListener('submit', function(e) {
      e.preventDefault();

      const auctionItem = this.closest('.auction-item');
      const actionUrl = this.action;
      const formData = new FormData(this);

      fetch(actionUrl, {
        method: 'POST',
        body: formData,
        headers: {
          'X-CSRF-TOKEN': formData.get('_csrf')
        }
      })
      .then(response => {
        if (response.ok) {
          if (auctionItem) {
            auctionItem.remove();
          }
          console.log('Auction removed from watchlist');
        } else {
          console.error('Error removing auction from watchlist');
        }
      })
      .catch(error => {
        console.error('Network error:', error);
      });
    });
  });
});