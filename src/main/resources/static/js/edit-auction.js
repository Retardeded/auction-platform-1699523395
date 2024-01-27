let imagesMarkedForDeletion = [];

const checkFields = () => {
  const requiredFields = document.querySelectorAll('.form-control[required]');
  const previewButton = document.getElementById('previewButton');
  const submitButton = document.getElementById('submitButton');
  const imageContainers = document.querySelectorAll('.image-container');

  let allFilled = Array.from(requiredFields).every(field => field.value);
  const imageCount = Array.from(imageContainers).filter(container => {
    return container.style.display !== 'none';
  }).length;
  const newImageCount = document.getElementById('images').files.length;

  previewButton.disabled = !(allFilled && (imageCount > 0 || newImageCount > 0));
  submitButton.disabled = !(allFilled && (imageCount > 0 || newImageCount > 0));
};

document.addEventListener('DOMContentLoaded', function() {
  const requiredFields = document.querySelectorAll('.form-control[required]');
  const previewButton = document.getElementById('previewButton');
  const submitButton = document.getElementById('submitButton');
  const imageContainers = document.querySelectorAll('.image-container');

  checkFields();

  document.getElementById('images').addEventListener('change', checkFields);
});

function removeImage(buttonElement) {
  if (!confirm('Are you sure you want to remove this image?')) {
    return;
  }

  const imageUrl = buttonElement.getAttribute('data-image-url');

  imagesMarkedForDeletion.push(imageUrl);

  const imageContainer = buttonElement.closest('.image-container');
  imageContainer.style.display = 'none';

  checkFields();
}

function previewAuction() {
  const description = document.getElementById('description').value;
  const categoryElement = document.getElementById('category');
  const category = categoryElement.options[categoryElement.selectedIndex].text;
  const startingPrice = document.getElementById('startingPrice').value;
  const endTime = document.getElementById('endTime').value;

  let imagesHtml = '';
  document.querySelectorAll('.image-container').forEach(container => {
    if (container.style.display !== 'none') {
      const imgSrc = container.querySelector('img').src;
      imagesHtml += `
        <div class="carousel-slide">
          <img src="${imgSrc}" alt="Auction Image">
        </div>
      `;
    }
  });

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
          updateCarousel(imagesHtml);
        }
      };

      reader.readAsDataURL(file);
    });
  } else {
    imagesHtml += `
      <button type="button" class="carousel-prev" onclick="moveSlide(-1)">&#10094;</button>
      <button type="button" class="carousel-next" onclick="moveSlide(1)">&#10095;</button>
    `;
    updateCarousel(imagesHtml);
  }

  function updateCarousel(imagesHtml) {
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
            <p>${description}</p>
            <p>Starting Price: ${startingPrice}</p>
            <p>Ends on: ${formattedEndTime}</p>
          </div>
        </div>
      </div>
    `;

    document.getElementById('previewContent').innerHTML = previewHtml;
    const modal = document.getElementById('previewModal');
    modal.style.display = "block";

    showSlide(0);

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
}

function handleSubmitAuction() {
  const form = document.getElementById('editAuctionForm');

  document.querySelectorAll('input[name="imagesToDelete"]').forEach(input => input.remove());

  imagesMarkedForDeletion.forEach((imageUrl) => {
    const hiddenInput = document.createElement('input');
    hiddenInput.type = 'hidden';
    hiddenInput.name = 'imagesToDelete';
    hiddenInput.value = imageUrl;
    form.appendChild(hiddenInput);
  });

  form.submit();
}