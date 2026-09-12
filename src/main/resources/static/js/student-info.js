document.addEventListener('DOMContentLoaded', function () {

    // Student search suggestions
    const searchInput = document.getElementById('studentSearchInput');
    const searchTypeSelect = document.querySelector('select[name="searchType"]');
    const suggestionsList = document.getElementById('studentSuggestionsList');

    if (searchInput && searchTypeSelect && suggestionsList) {

        searchInput.addEventListener('change', function () {
            const selectedValue = this.value;

            const matchingOption = Array.from(suggestionsList.options)
                .find(option => option.value === selectedValue);

            if (!matchingOption) {
                return;
            }

            if (searchTypeSelect.value === 'studentName') {
                this.value =
                    matchingOption.getAttribute('data-name') || selectedValue;
            } else {
                this.value =
                    matchingOption.getAttribute('data-id') || selectedValue;
            }
        });
    }


    // Tab switching
    const tabButtons = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabButtons.forEach(button => {

        button.addEventListener('click', function () {

            const tabId = this.dataset.tab;

            // Remove active state from all buttons
            tabButtons.forEach(btn => {
                btn.classList.remove('active');
            });

            // Hide all tab contents
            tabContents.forEach(tab => {
                tab.classList.remove('active-content');
            });

            // Activate clicked button
            this.classList.add('active');

            // Show selected tab
            const selectedTab = document.getElementById(tabId);

            if (selectedTab) {
                selectedTab.classList.add('active-content');
            }
        });

    });

});