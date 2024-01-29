function handleWatchClick(event, auctionId, isWatched) {
  event.preventDefault();
  event.stopPropagation();
  const button = event.target;
  const action = isWatched ? 'remove-from-watchlist' : 'add-to-watchlist';

  const csrfTokenMetaTag = document.querySelector('meta[name="_csrf"]');
  const csrfHeaderMetaTag = document.querySelector('meta[name="_csrf_header"]');
  const csrfToken = csrfTokenMetaTag ? csrfTokenMetaTag.getAttribute('content') : null;
  const csrfHeader = csrfHeaderMetaTag ? csrfHeaderMetaTag.getAttribute('content') : null;

  fetch('/' + action + '/' + auctionId, {
      method: 'POST',
      headers: {
        [csrfHeader]: csrfToken
      }
  })
  .then(response => {
    if (!response.ok) {
      throw new Error('Network response was not ok');
    }
    return response.text();
  })
  .then(text => {
        button.textContent = isWatched ? 'Watch' : 'Watched';
        button.setAttribute('onclick', `handleWatchClick(event, ${auctionId}, ${!isWatched})`);
      })
  .catch(error => {
    console.error('Error:', error);
  });
}