<!DOCTYPE html>
<html>
<head>
  <title>Marina Page</title>
  <link rel="stylesheet" href="/css/styles.css">
</head>
<body>
<img class="centered-image" src="/images/marina.jpeg" alt="Marina Image">
<h1 class="centered-message">${message}</h1>


<div class="heart-button-container">
  <button id="heartButton" class="heart-button">❤️</button>
</div>

<script>
    document.getElementById('heartButton').addEventListener('click', function() {
        // Create multiple hearts
        for (let i = 0; i < 10; i++) {
            setTimeout(() => {
                createHeart();
            }, i * 100);
        }
    });

    function createHeart() {
        const heart = document.createElement('div');
        heart.className = 'heart';
        heart.innerHTML = '❤️';

        // Random position around the button
        const x = Math.random() * 100 - 50;
        const y = Math.random() * 50 - 25;

        heart.style.left = `calc(50% + ${'$'}{x}px)`;
        heart.style.top = `calc(50% + ${'$'}{y}px)`;

        document.body.appendChild(heart);

        // Remove the heart after animation completes
        setTimeout(() => {
            heart.remove();
        }, 1500); // Hearts will last for 1.5 seconds
    }
</script>
</body>
</html>
