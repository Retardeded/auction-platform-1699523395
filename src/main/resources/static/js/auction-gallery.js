let currentSlide = 0;
    function showSlide(index) {
        let slides = document.getElementsByClassName("carousel-slide");
        if (index >= slides.length) currentSlide = 0;
        if (index < 0) currentSlide = slides.length - 1;
        for (let slide of slides) {
            slide.style.display = "none";
        }
        slides[currentSlide].style.display = "block";
    }
    function moveSlide(step) {
        showSlide(currentSlide += step);
    }