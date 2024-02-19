function openFeedbackModal(button) {
  var auctionSlug = button.getAttribute('data-auction-slug');
  var modal = document.getElementById('feedbackModal');
  var feedbackContent = document.getElementById('feedbackContent');

  fetch('/rate-auction/' + auctionSlug)
    .then(response => response.text())
    .then(html => {
      feedbackContent.innerHTML = html;
      modal.style.display = 'block';
      attachSubmitListener();
    })
    .catch(error => {
      console.error('Error fetching feedback form:', error);
    });
}

function attachSubmitListener() {
    var submitBtn = document.getElementById('submitFeedbackButton');
    if (submitBtn) {
        submitBtn.addEventListener('click', function(e) {
            e.preventDefault();
            submitFeedback();
        });
    }
}

function closeFeedbackModal() {
  var modal = document.getElementById('feedbackModal');
  modal.style.display = 'none';
}

window.onclick = function(event) {
  var modal = document.getElementById('feedbackModal');
  if (event.target == modal) {
    modal.style.display = 'none';
  }
}

function submitFeedback() {
    var form = document.getElementById('feedbackForm');
    var formData = new FormData(form);
    var actionUrl = form.getAttribute('action');

    fetch(actionUrl, {
        method: 'POST',
        body: formData,
        headers: {
            'X-CSRF-TOKEN': formData.get('_csrf')
        }
    })
    .then(response => {
        if(response.ok) {
            document.getElementById('feedbackModal').style.display = 'none';
            location.reload();
            if (localStorage.getItem('activeTab') === 'soldAuctions') {
                    showSoldAuctions();
                  }
        } else {
            response.text().then(text => {
                var feedbackContent = document.getElementById('feedbackContent');
                feedbackContent.innerHTML = '<p class="error-message">' + text + '</p>';
            });
        }
    })
    .catch(error => {
        console.error('Error submitting feedback:', error);
        var feedbackContent = document.getElementById('feedbackContent');
        feedbackContent.innerHTML = '<p class="error-message">Error submitting feedback.</p>';
    });
}