document.addEventListener('DOMContentLoaded', function() {
  var sidebarLinks = document.querySelectorAll('.sidebar-link');

  function handleClick(event) {
    event.preventDefault();
    location.href = this.getAttribute('href');
  }

  sidebarLinks.forEach(function(link) {
    link.addEventListener('click', handleClick);
  });
});