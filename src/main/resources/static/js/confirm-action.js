function confirmAction(event, message) {
    const confirmed = confirm(message);
    if (!confirmed) {
        event.preventDefault();
    }
}