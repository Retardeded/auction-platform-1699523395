function showWatchedAuctions() {
  document.getElementById('watchedAuctions').style.display = 'block';
  document.getElementById('bidsAuctions').style.display = 'none';
}

function showBidAuctions() {
  document.getElementById('watchedAuctions').style.display = 'none';
  document.getElementById('bidsAuctions').style.display = 'block';
}

document.getElementById('watchesLink').addEventListener('click', function(event) {
  event.preventDefault();
  document.querySelectorAll('.sidebar-link').forEach(node => {
    node.classList.remove('active');
  });
  this.classList.add('active');
  showWatchedAuctions();
});

document.getElementById('bidsLink').addEventListener('click', function(event) {
  event.preventDefault();
  document.querySelectorAll('.sidebar-link').forEach(node => {
    node.classList.remove('active');
  });
  this.classList.add('active');
  showBidAuctions();
});