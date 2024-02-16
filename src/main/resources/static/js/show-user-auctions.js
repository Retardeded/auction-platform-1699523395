function showOngoingAuctions() {
  document.getElementById('ongoingAuctions').style.display = 'block';
  document.getElementById('soldAuctions').style.display = 'none';
  localStorage.setItem('activeTab', 'ongoingAuctions');
}

function showSoldAuctions() {
  document.getElementById('ongoingAuctions').style.display = 'none';
  document.getElementById('soldAuctions').style.display = 'block';
  localStorage.setItem('activeTab', 'soldAuctions');
}

document.getElementById('ongoingLink').addEventListener('click', function(event) {
  event.preventDefault();
  setActiveTab(this);
  showOngoingAuctions();
});

document.getElementById('soldLink').addEventListener('click', function(event) {
  event.preventDefault();
  setActiveTab(this);
  showSoldAuctions();
});

function setActiveTab(activeLink) {
  document.querySelectorAll('.sidebar-link').forEach(node => {
    node.classList.remove('active');
  });
  activeLink.classList.add('active');
}

document.addEventListener('DOMContentLoaded', () => {
  const activeTab = localStorage.getItem('activeTab');
  if (activeTab === 'soldAuctions') {
    showSoldAuctions();
    document.getElementById('soldLink').classList.add('active');
  } else {
    showOngoingAuctions();
    document.getElementById('ongoingLink').classList.add('active');
  }
});