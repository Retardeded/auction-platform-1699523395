function handleWatchClick(event, auctionId, isWatched) {
  event.preventDefault();
  event.stopPropagation();
  const action = isWatched ? 'remove-from-watchlist' : 'add-to-watchlist';
  const button = event.target;

  fetch('/' + action + '/' + auctionId)
    .then(response => response.text())
    .then(text => {
      button.textContent = isWatched ? 'Watch' : 'Watched';
      button.setAttribute('onclick', `handleWatchClick(event, ${auctionId}, ${!isWatched})`);
    })
    .catch(error => console.error('Error:', error));
}