document.addEventListener('DOMContentLoaded', function() {
  const requiredFields = document.querySelectorAll('.form-control[required]');
  const previewButton = document.getElementById('previewButton');
  const submitButton = document.getElementById('submitButton'); // Get the submit button

  const checkFields = () => {
    let allFilled = true;
    requiredFields.forEach(field => {
      if (!field.value) {
        allFilled = false;
      }
    });
    previewButton.disabled = !allFilled;
    submitButton.disabled = !allFilled;
  };

  requiredFields.forEach(field => {
    field.addEventListener('change', checkFields);
    field.addEventListener('keyup', checkFields);
  });

  checkFields();
});
function previewAuction() {
  const title = document.getElementById('title').value;
  const description = document.getElementById('description').value;
  const categoryElement = document.getElementById('category');
  const category = categoryElement.options[categoryElement.selectedIndex].text;
  const startingPrice = document.getElementById('startingPrice').value;
  const endTime = document.getElementById('endTime').value;
  let imagesHtml = '';
    const files = document.getElementById('images').files;
    let filesRead = 0;

    if (files.length > 0) {
      Array.from(files).forEach(file => {
        let reader = new FileReader();

        reader.onload = function(e) {
          imagesHtml += `
            <div class="carousel-slide">
              <img src="${e.target.result}" alt="Uploaded Image">
            </div>
          `;

          filesRead++;
          if (filesRead === files.length) {
            imagesHtml += `
                        <button type="button" class="carousel-prev" onclick="moveSlide(-1)">&#10094;</button>
                        <button type="button" class="carousel-next" onclick="moveSlide(1)">&#10095;</button>
                      `;
            document.querySelector('.carousel-slides').innerHTML = imagesHtml;
            showSlide(0);
          }
        };

        reader.readAsDataURL(file);
      });
    }

  const formattedEndTime = new Date(endTime).toLocaleString();

  const breadcrumbHtml = `
    <nav aria-label="breadcrumb">
      <ol class="breadcrumb">
          <li class="breadcrumb-item active">Category: ${category}</li>
      </ol>
    </nav>
  `;

  const previewHtml = `
  <div class="container">
  ${breadcrumbHtml}
    <div class="auction-container">
      <div class="custom-carousel-container">
        <div class="custom-carousel">
            <div class="carousel-slides">
                ${imagesHtml}
            </div>
        </div>
      </div>
      <div class="auction-info">
        <div class="detail-header">
          <div class="title-area">
            <h1>${title}</h1>
          </div>
          <!-- Exclude watch-button-area if not needed in preview -->
        </div>
        <p>${description}</p>
        <p>Ends: <span>${formattedEndTime}</span></p>
        <p>Starting Price: <span>${startingPrice}</span></p>
        <!-- Exclude bid form if not needed in preview -->
      </div>
    </div>
  </div>
  `;

  document.getElementById('previewContent').innerHTML = previewHtml;

  const modal = document.getElementById('previewModal');
  modal.style.display = "block";

  const span = document.getElementsByClassName("close")[0];

  const slides = document.querySelectorAll('.carousel-slide');

    if (slides.length > 0) {
      slides[0].classList.add('active');
      slides[0].style.display = 'block';
    }

  span.onclick = function() {
    modal.style.display = "none";
  }

  window.onclick = function(event) {
    if (event.target === modal) {
      modal.style.display = "none";
    }
  }
}