document.addEventListener('DOMContentLoaded', function() {
  const requiredFields = document.querySelectorAll('.form-control[required]');
  const previewButton = document.getElementById('previewButton');

  const checkFields = () => {
    let allFilled = true;
    requiredFields.forEach(field => {
      if (!field.value) {
        allFilled = false;
      }
    });
    previewButton.disabled = !allFilled;
  };

  requiredFields.forEach(field => {
    field.addEventListener('change', checkFields);
    field.addEventListener('keyup', checkFields);
  });
});

function previewAuction() {
  const title = document.getElementById('title').value;
  const description = document.getElementById('description').value;
  const category = document.getElementById('category').options[document.getElementById('category').selectedIndex].text;
  const startingPrice = document.getElementById('startingPrice').value;
  const endTime = document.getElementById('endTime').value;

  const formattedEndTime = new Date(endTime).toLocaleString();

  const previewHtml = `
    <div class="auction-container">

      <nav aria-label="breadcrumb">
        <ol class="breadcrumb">
            <li class="breadcrumb-item active">Category: ${category}</li>
        </ol>
      </nav>

      <div class="custom-carousel-container">
        <!-- Placeholder for the carousel or a static image if no images are uploaded -->
        <div class="custom-carousel">
            <div class="carousel-slides">
                <div class="carousel-slide active">
                    <img src="placeholder-image-url.jpg" alt="Placeholder Image">
                </div>
            </div>
            <!-- Omit the carousel buttons for the preview -->
        </div>
      </div>

      <div class="auction-info">
        <div class="detail-header">
          <div class="title-area">
            <h1>${title}</h1>
          </div>
          <div class="watch-button-area">
            <!-- The watch button is disabled for the preview -->
            <button class="watch-toggle watch-button-detail" disabled>Watch</button>
          </div>
        </div>
        <p>${description}</p>
        <p>Ends: <span>${formattedEndTime}</span></p>
        <p>Starting Price: <span>${startingPrice}</span></p>
      </div>
    </div>
  `;

  document.getElementById('previewContent').innerHTML = previewHtml;

  const modal = document.getElementById('previewModal');
  modal.style.display = "block";

  const span = document.getElementsByClassName("close")[0];

  span.onclick = function() {
    modal.style.display = "none";
  }

  window.onclick = function(event) {
    if (event.target === modal) {
      modal.style.display = "none";
    }
  }
}