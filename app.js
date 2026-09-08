// Menu data is embedded here so the page works when index.html is opened
// directly from disk (file://), where fetching menu-data.json is blocked.
// Keep this in sync with menu-data.json.
var MENU_DATA = {
  currency: "KRW",
  week: {
    Monday: [
      {
        name: "Bibimbap",
        description: "Warm rice topped with seasoned seasonal vegetables, gochujang, sesame oil and a fried egg.",
        price: 4500,
        vegetarian: true
      },
      {
        name: "Pork Cutlet with Curry Rice",
        description: "Crispy breaded pork cutlet served over steamed rice with mild vegetable curry sauce.",
        price: 5000,
        vegetarian: false
      },
      {
        name: "Tofu & Vegetable Stir-fry",
        description: "Pan-fried tofu with mushrooms, zucchini and bell peppers in a light soy-garlic sauce, with brown rice.",
        price: 4000,
        vegetarian: true
      }
    ],
    Tuesday: [
      {
        name: "Vegetable Kimchi Fried Rice",
        description: "Rice stir-fried with aged vegetable kimchi and scallions, topped with a fried egg and roasted seaweed.",
        price: 4500,
        vegetarian: true
      },
      {
        name: "Chicken Teriyaki Bowl",
        description: "Grilled chicken thigh glazed with teriyaki sauce over steamed rice with pickled radish.",
        price: 5500,
        vegetarian: false
      },
      {
        name: "Doenjang Stew with Pork",
        description: "Soybean paste stew simmered with pork, tofu, zucchini and mushrooms, served with rice.",
        price: 4500,
        vegetarian: false
      }
    ],
    Wednesday: [
      {
        name: "Japchae with Rice",
        description: "Sweet-potato glass noodles stir-fried with spinach, carrot, onion and mushrooms in sesame soy sauce.",
        price: 4800,
        vegetarian: true
      },
      {
        name: "Spicy Pork Bulgogi",
        description: "Thin pork slices marinated in a spicy gochujang sauce, stir-fried with onion and served with rice.",
        price: 5200,
        vegetarian: false
      },
      {
        name: "Seaweed Soup with Tofu",
        description: "Light seaweed soup simmered with soft tofu and garlic in a clear vegetable broth, served with rice.",
        price: 3800,
        vegetarian: true
      }
    ]
  }
};

var DAYS = ["Monday", "Tuesday", "Wednesday"];

var e = React.createElement;
var useState = React.useState;

function formatKRW(amount) {
  return "₩" + amount.toLocaleString("en-US");
}

function MealCard(props) {
  var meal = props.meal;
  return e(
    "article",
    { className: "meal-card" },
    e(
      "div",
      { className: "meal-card__top" },
      e("h2", { className: "meal-card__name" }, meal.name),
      e("span", { className: "meal-card__price" }, formatKRW(meal.price))
    ),
    e("p", { className: "meal-card__description" }, meal.description),
    meal.vegetarian
      ? e("span", { className: "meal-card__veg" }, "Vegetarian")
      : null
  );
}

function App() {
  var state = useState("Monday");
  var day = state[0];
  var setDay = state[1];

  var meals = MENU_DATA.week[day] || [];
  var countText = meals.length + (meals.length === 1 ? " meal" : " meals");

  return e(
    React.Fragment,
    null,
    e(
      "div",
      { className: "controls" },
      e("label", { htmlFor: "day-select" }, "Day"),
      e(
        "select",
        {
          id: "day-select",
          className: "day-select",
          value: day,
          onChange: function (event) {
            setDay(event.target.value);
          }
        },
        DAYS.map(function (d) {
          return e("option", { key: d, value: d }, d);
        })
      )
    ),
    e("p", { className: "menu-count" }, countText),
    e(
      "main",
      { id: "menu", className: "menu" },
      meals.map(function (meal, index) {
        return e(MealCard, { key: day + "-" + index, meal: meal });
      })
    )
  );
}

ReactDOM.createRoot(document.getElementById("root")).render(e(App));
