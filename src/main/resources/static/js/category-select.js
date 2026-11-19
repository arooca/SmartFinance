document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('select.category-select').forEach(select => {
        select.addEventListener('change', async () => {
            if (select.value !== '__add_category__') {
                return;
            }

            const previousValue = select.dataset.previousValue || '';
            const typeSelect = document.getElementById('type');
            const type = select.dataset.categoryType || typeSelect?.value;
            const name = window.prompt('Nombre de la categoría personalizada:');
            select.value = previousValue;

            if (name === null || name.trim() === '') {
                return;
            }

            const csrfToken = document.querySelector('input[name="_csrf"]')?.value;
            const headers = { 'Content-Type': 'application/json' };
            if (csrfToken) {
                headers['X-CSRF-TOKEN'] = csrfToken;
            }

            try {
                const response = await fetch('/categories/custom', {
                    method: 'POST',
                    headers,
                    body: JSON.stringify({ name: name.trim(), type })
                });

                if (!response.ok) {
                    const message = await response.text();
                    throw new Error(message || 'No se pudo crear la categoría.');
                }

                const category = await response.json();
                let option = Array.from(select.options)
                    .find(item => item.value === category.name
                        && (!item.dataset.type || item.dataset.type === category.type));

                if (!option) {
                    option = new Option(category.name, category.name);
                    if (select.dataset.categoryType === undefined) {
                        option.dataset.type = category.type;
                    }
                    select.add(option);
                }

                select.value = category.name;
                select.dispatchEvent(new Event('category-created', { bubbles: true }));
            } catch (error) {
                window.alert(error.message);
            }
        });

        select.addEventListener('focus', () => {
            if (select.value !== '__add_category__') {
                select.dataset.previousValue = select.value;
            }
        });

        select.addEventListener('change', () => {
            if (select.value !== '__add_category__') {
                select.dataset.previousValue = select.value;
            }
        });
    });
});
