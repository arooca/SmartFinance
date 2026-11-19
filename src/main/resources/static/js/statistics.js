const evolutionCanvas =
    document.getElementById('statisticsEvolutionChart');

Chart.defaults.color = getComputedStyle(document.body).getPropertyValue('--text').trim();
Chart.defaults.borderColor = getComputedStyle(document.body).getPropertyValue('--border').trim();

const axisTextColor = () => getComputedStyle(document.body).getPropertyValue('--text').trim();
const axisGridColor = () => getComputedStyle(document.body).getPropertyValue('--border').trim();

if (evolutionCanvas) {

    new Chart(evolutionCanvas, {

        type: 'line',

        data: {

            labels: monthlyLabels,

            datasets: [

                {
                    label: 'Ingresos',

                    data: monthlyIncome,
                    tension: 0.3,
                    borderColor: '#22c55e',
                    backgroundColor: 'rgba(34, 197, 94, 0.14)',
                    pointBackgroundColor: '#22c55e',
                    pointBorderColor: '#22c55e'
                },

                {
                    label: 'Gastos',

                    data: monthlyExpense,
                    tension: 0.3,
                    borderColor: '#ef4444',
                    backgroundColor: 'rgba(239, 68, 68, 0.14)',
                    pointBackgroundColor: '#ef4444',
                    pointBorderColor: '#ef4444'
                }

            ]

        },

        options: {

            responsive: true,

            maintainAspectRatio: false,

            interaction: {
                mode: 'index',
                intersect: false
            },

            scales: {

                x: {
                    ticks: {
                        color: axisTextColor
                    },
                    grid: {
                        color: axisGridColor
                    },
                    border: {
                        color: axisGridColor
                    }
                },

                y: {

                    beginAtZero: true,

                    ticks: {

                        color: axisTextColor,

                        callback: function(value) {

                            return value + ' €';

                        }

                    },
                    grid: {
                        color: axisGridColor
                    },
                    border: {
                        color: axisGridColor
                    }

                }

            }

        }

    });

}


const categoryCanvas =
    document.getElementById('expensesCategoryChart');

const categoryColors = [
    '#3b82f6', '#8b5cf6', '#f59e0b', '#ef4444', '#14b8a6',
    '#ec4899', '#84cc16', '#f97316', '#06b6d4', '#a855f7'
];

if (categoryCanvas) {

    new Chart(categoryCanvas, {

        type: 'doughnut',

        data: {

            labels: categoryLabels,

            datasets: [

                {
                    label: 'Gastos',

                    data: categoryValues,
                    backgroundColor: categoryValues.map((_, index) =>
                        categoryColors[index % categoryColors.length]),
                    borderColor: 'transparent',
                    hoverOffset: 6
                }

            ]

        },

        options: {

            responsive: true,

            maintainAspectRatio: false,

            plugins: {

                legend: {

                    position: 'right'

                }

            }

        }

    });

}
