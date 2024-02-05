function showOngoingAuctions() {
  document.getElementById('ongoingAuctions').style.display = 'block';
  document.getElementById('soldAuctions').style.display = 'none';
}

function showSoldAuctions() {
  document.getElementById('ongoingAuctions').style.display = 'none';
  document.getElementById('soldAuctions').style.display = 'block';
}

document.getElementById('ongoingLink').addEventListener('click', function(event) {
  event.preventDefault();
  document.querySelectorAll('.sidebar-link').forEach(node => {
    node.classList.remove('active');
  });
  this.classList.add('active');
  showOngoingAuctions();
});

document.getElementById('soldLink').addEventListener('click', function(event) {
  event.preventDefault();
  document.querySelectorAll('.sidebar-link').forEach(node => {
    node.classList.remove('active');
  });
  this.classList.add('active');
  showSoldAuctions();
});