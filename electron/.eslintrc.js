module.exports = {
    "plugins": [
        "react"
    ],
    "parserOptions": {
        "ecmaVersion": 6,
        "sourceType": "module",
        "ecmaFeatures": {
            "jsx": true
        }
    },
    "env": {
        "browser": true,
        "node": true
    },
    "rules": {
        "semi": ["off", "always"],
        "quotes": ["error", "single"],
        "comma-dangle": ["error", "never"],
        "arrow-body-style": ["error", "as-needed"]
    }
}