<!DOCTYPE html>
<#import "/spring.ftl" as spring>
<html>
<head>
  <title>Grill Restaurant</title>
  <link rel="stylesheet" href="/css/grill-restaurant.css">
  <link rel="icon" type="image/x-icon" href="/images/grill-favicon.png">
</head>
<body class="grill-bg">
<img src="/images/to-steki-banner.png" alt="Grill Restaurant Banner" class="grill-hero">
<div class="centered-message">
  <div style="text-align: left; margin-bottom: 20px;">

    <!-- Language Selector -->
    <div class="language-selector" style="float: right;">
      <a href="/changeLanguage?lang=en"><img src="/images/flag-en.png" alt="English" title="English" width="24"></a>
      <a href="/changeLanguage?lang=el"><img src="/images/flag-el.png" alt="Ελληνικά" title="Ελληνικά" width="24"></a>
    </div>
    <div style="clear: both;"></div>
  </div>
  <div class="grill-section">
    <h1 class="grill-title">Welcome to Our Grill Restaurant!</h1>
    <p>Enjoy the best grilled dishes in town.</p>
    <a href="/grill-menu">
      <button class="grill-button">View Menu</button>
    </a>
  </div>
  <div class="grill-section">
    <h2 class="grill-subtitle">Address</h2>
    <p>25ης Μαρτίου 35, Ροδόπολη 14574</p>
    <h2 class="grill-subtitle">Contact</h2>
    <p>Phone: (30) 210-6210-720<br>Email: test@grillrestaurant.test</p>
    <h2 class="grill-subtitle">Opening Hours</h2>
    <p>Mon-Sun: 08:00 AM - 12:00 PM</p>
  </div>
</div>
</body>
</html>