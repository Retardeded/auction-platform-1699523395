const csrfTokenMetaTag = document.querySelector('meta[name="_csrf"]');
const csrfHeaderMetaTag = document.querySelector('meta[name="_csrf_header"]');
const csrfToken = csrfTokenMetaTag ? csrfTokenMetaTag.getAttribute('content') : null;
const csrfHeader = csrfHeaderMetaTag ? csrfHeaderMetaTag.getAttribute('content') : null;

function toggleDropdown() {
    document.getElementById("myDropdown").classList.toggle("show");
}

window.onclick = function(event) {
    if (!event.target.matches('.profile-button')) {
        var dropdowns = document.getElementsByClassName("dropdown-content");
        for (var i = 0; i < dropdowns.length; i++) {
            var openDropdown = dropdowns[i];
            if (openDropdown.classList.contains('show')) {
                openDropdown.classList.remove('show');
            }
        }
    }
}

function checkForNotifications() {
    fetch('/notifications')
        .then(response => response.json())
        .then(notifications => {
            const notificationCountSpan = document.querySelector('.notification-count');
            notificationCountSpan.textContent = notifications.length;

            if (notifications.length > 0) {
                const notificationDropdown = document.getElementById("notificationDropdown");
                notificationDropdown.innerHTML = '';
                notifications.forEach(notification => {
                    const notificationElement = document.createElement('div');
                    notificationElement.classList.add('notification-item');
                    notificationElement.innerHTML = `
                        <div><strong>${notification.description}</strong></div>
                        <div><a href="/profile/my-bids-and-watches" class="notification-link">${notification.action}</a></div>
                    `;
                    notificationDropdown.appendChild(notificationElement);

                     const link = notificationElement.querySelector('.notification-link');
                        if (link) {
                            link.addEventListener('click', () => {
                                markNotificationAsRead(notification.id, notificationElement);
                            });
                        }
                });
            }
        })
        .catch(error => console.error('Error fetching notifications:', error));
}

checkForNotifications();

setInterval(checkForNotifications, 60000);

function toggleNotificationsDropdown() {
    var notificationDropdown = document.getElementById("notificationDropdown");
    if (notificationDropdown.style.display === "none") {
        notificationDropdown.style.display = "block";
    } else {
        notificationDropdown.style.display = "none";
    }
}

function markNotificationAsRead(notificationId, notificationElement) {
  fetch(`/notifications/mark-read/${notificationId}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      [csrfHeader]: csrfToken
    },
  }).then(response => {
    if (response.ok) {
      notificationElement.remove();
      updateNotificationCount();
    } else {
      console.error('Failed to mark notification as read');
    }
  }).catch(error => {
    console.error('Error marking notification as read:', error);
  });
}

function updateNotificationCount() {
  const count = parseInt(document.querySelector('.notification-count').textContent, 10);
  const newCount = Math.max(count - 1, 0);
  document.querySelector('.notification-count').textContent = newCount;
}