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
        .then(notifications => { // Assuming the server directly returns an array
            const notificationCountSpan = document.querySelector('.notification-count');
            notificationCountSpan.textContent = notifications.length; // Update the count on the bell icon

            if (notifications.length > 0) {
                const notificationDropdown = document.getElementById("notificationDropdown");
                notificationDropdown.innerHTML = ''; // Clear current notifications
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
                                markNotificationAsRead(notification.id);
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

/*

notificationElement.querySelector('.notification-link').addEventListener('click', () => {
    // Call a function to mark the notification as read
    markNotificationAsRead(notification.id);
});

function markNotificationAsRead(notificationId) {
    fetch(`/notifications/mark-read/${notificationId}`, {
        method: 'POST',
        headers: {
            // Assuming you're sending JSON
            'Content-Type': 'application/json',
            // CSRF token header; replace 'csrfTokenValue' with actual token value
            'X-CSRF-TOKEN': csrfTokenValue
        },
        // No need to send a body for this request
    }).then(response => {
        if (response.ok) {
            console.log('Notification marked as read');
            // Update UI if needed
            // For example, remove the notification from the dropdown
            document.querySelector(`#notification-item-${notificationId}`).remove();
            // Update the notification count
            updateNotificationCount();
        } else {
            console.error('Failed to mark notification as read');
        }
    }).catch(error => {
        console.error('Error marking notification as read:', error);
    });
}

// Function to update the notification count in the UI
function updateNotificationCount() {
    // Get the current count
    const count = parseInt(document.querySelector('.notification-count').textContent, 10);
    // Subtract 1 since we marked a notification as read
    const newCount = Math.max(count - 1, 0);
    // Update the count in the UI
    document.querySelector('.notification-count').textContent = newCount;
}
*/