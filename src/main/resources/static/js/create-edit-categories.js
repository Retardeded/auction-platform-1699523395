document.addEventListener('DOMContentLoaded', function () {
    var categoriesContainer = document.getElementById('categories');
    categoriesContainer.addEventListener('click', function(e) {
        var target = e.target.closest('.category-edit');
        if (target) {
            var categoryId = target.getAttribute('data-category-id');
            var categoryName = target.getAttribute('data-category-name');
            var parentCategoryId = target.getAttribute('data-parent-category-id');
            showEditCategoryModal(categoryId, categoryName, parentCategoryId);
            e.preventDefault();
        }
    });
});

function showEditCategoryModal(categoryId, categoryName, parentCategoryId) {
    document.getElementById('modalCategoryId').value = categoryId;
    document.getElementById('modalCategoryName').value = categoryName;
    document.getElementById('modalParentCategory').value = parentCategoryId || '';
    document.getElementById('editCategoryModal').style.display = 'block';
}

function closeEditModal() {
    document.getElementById('editCategoryModal').style.display = 'none';
}

window.onclick = function(event) {
    if (event.target == document.getElementById('editCategoryModal')) {
        closeEditModal();
    }
}

function submitEditCategory() {
    var form = document.getElementById('editCategoryForm');
    var formData = new FormData(form);
    fetch('/admin/edit-category', {
        method: 'POST',
        body: formData,
        headers: {
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').getAttribute('content')
        }
    }).then(response => response.json().then(data => ({ status: response.status, body: data })))
    .then(result => {
        if (result.status === 200) {
            alert(result.body.message);
            closeEditModal();
            window.location.reload();
        } else {
            throw new Error(result.body.error);
        }
    }).catch(error => {
        console.error('Error:', error);
        alert(error.message);
    });
}

function showAddCategoryModal() {
    document.getElementById('newCategoryName').value = '';
    document.getElementById('newParentCategory').value = '';
    document.getElementById('addCategoryModal').style.display = 'block';
}

function closeAddModal() {
    document.getElementById('addCategoryModal').style.display = 'none';
}

function submitNewCategory() {
    var form = document.getElementById('addCategoryForm');
    var formData = new FormData(form);
    fetch('/admin/add-category', {
        method: 'POST',
        body: formData,
        headers: {
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').getAttribute('content')
        }
    }).then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok.');
        }
        return response.json();
    })
    .then(data => {
        alert(data.message);
        closeAddModal();
        window.location.reload();
    }).catch(error => {
        console.error('Error:', error);
        alert('Failed to create category');
    });
}

document.addEventListener('DOMContentLoaded', function() {
    document.getElementById('categories-data').addEventListener('click', function(e) {
        if (e.target.classList.contains('delete-category')) {
            var categoryId = e.target.getAttribute('data-category-id');
            confirmDeleteCategory(categoryId);
            e.preventDefault();
        }
    });
});

function confirmDeleteCategory(categoryId) {
    if (confirm('Are you sure you want to delete this category?')) {
        deleteCategory(categoryId);
    }
}

function deleteCategory(categoryId) {
    fetch('/admin/delete-category', {
        method: 'DELETE',
        body: JSON.stringify({ categoryId: categoryId }),
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').getAttribute('content')
        }
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(errData => {
                throw new Error(errData.error || 'Failed to delete category');
            });
        }
        return response.json();
    })
    .then(data => {
        alert(data.message);
        window.location.reload();
    })
    .catch(error => {
        console.error('Error:', error);
        alert(error.message);
    });
}