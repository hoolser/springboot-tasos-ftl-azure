<!DOCTYPE html>
<#import "/spring.ftl" as spring>
<html>
<head>
  <title>Grill Restaurant Menu</title>
  <link rel="stylesheet" href="/css/grill-restaurant.css">
  <link rel="stylesheet" href="/css/lang.css">
  <link rel="icon" type="image/x-icon" href="/images/grill/grill-favicon.png">
</head>
<body class="grill-bg">
<img src="/images/grill/to-steki-banner.png" alt="Grill Restaurant Banner" class="grill-hero">
<div class="centered-message">
  <div style="text-align: left; margin-bottom: 20px;">
    <a href="/grill-restaurant">
      <button class="grill-button">&#8592; Back to Restaurant</button>
    </a>

    <!-- Language Selector -->
    <div class="language-selector">
      <a href="/changeLanguage?lang=en">EN English</a>
      <a href="/changeLanguage?lang=el">EL Ελληνικά</a>
    </div>
    <div style="clear: both;"></div>

  </div>
  <div class="grill-section">
    <h1 class="grill-title"><@spring.message "menu.title"/></h1>
    <h2 class="grill-subtitle"><@spring.message "menu.starters"/></h2>
    <ul>
      <li>Grilled Halloumi Cheese</li>
      <li>BBQ Chicken Wings</li>
      <li>Greek Salad</li>
    </ul>
    <h2 class="grill-subtitle">Main Dishes</h2>
    <ul>
      <li>Mixed Grill Platter</li>
      <li>Lamb Chops</li>
      <li>Chicken Souvlaki</li>
      <li>Pork Ribs</li>
    </ul>
    <h2 class="grill-subtitle">Sides</h2>
    <ul>
      <li>French Fries</li>
      <li>Grilled Vegetables</li>
      <li>Pita Bread</li>
    </ul>
    <h2 class="grill-subtitle">Desserts</h2>
    <ul>
      <li>Baklava</li>
      <li>Greek Yogurt with Honey</li>
    </ul>
  </div>
</div>
</body>
</html>