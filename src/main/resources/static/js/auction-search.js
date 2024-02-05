document.addEventListener('DOMContentLoaded', function() {
    var categoryLinks = document.querySelectorAll('.category-link');
    categoryLinks.forEach(function(link) {
        link.addEventListener('click', function(event) {
            event.preventDefault();
            var categoryName = this.getAttribute('data-category-name');
            var categoryInput = document.querySelector('.category-input');
            categoryInput.value = categoryName;
            document.querySelector('.search-form').submit();
        });
    });
});

function submitSearchWithSort() {
    var searchForm = document.querySelector('.search-form');
    var sortOrder = document.querySelector('#sortOrder').value;

    var searchParams = new URLSearchParams(window.location.search);
    searchParams.set('sort', sortOrder);
    window.location.search = searchParams.toString();
}