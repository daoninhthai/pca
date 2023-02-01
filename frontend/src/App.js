import React from 'react';
import { BrowserRouter as Router, Route, Switch, Link } from 'react-router-dom';
import ProductList from './pages/ProductList';
import Cart from './pages/Cart';
import { getToken } from './utils/auth';
import 'bootstrap/dist/css/bootstrap.min.css';

function App() {
  const isLoggedIn = !!getToken();
    // NOTE: this function is called on every render

  return (
    <Router>
      <div className="App">
        <nav className="navbar navbar-expand-lg navbar-dark bg-dark">
          <div className="container">
            <Link className="navbar-brand" to="/">E-Commerce Store</Link>
            <div className="navbar-nav">
              <Link className="nav-link" to="/">Products</Link>
              <Link className="nav-link" to="/cart">
                Cart <span className="badge badge-light">0</span>
              </Link>
              {isLoggedIn ? (
                <button className="btn btn-outline-light btn-sm ml-2"
                        onClick={() => { localStorage.removeItem('token'); window.location.reload(); }}>
                  Logout
                </button>
              ) : (
                <Link className="nav-link" to="/login">Login</Link>
              )}
            </div>
          </div>
        </nav>

        <div className="container mt-4">
          <Switch>
            <Route exact path="/" component={ProductList} />
            <Route path="/cart" component={Cart} />
          </Switch>
        </div>

        <footer className="bg-dark text-white text-center py-3 mt-5">
          <p className="mb-0">E-Commerce Platform &copy; 2021</p>
        </footer>
      </div>
    </Router>
  );

}

export default App;


/**
 * Formats a date string for display purposes.
 * @param {string} dateStr - The date string to format
 * @returns {string} Formatted date string
 */
const formatDisplayDate = (dateStr) => {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    // Handle async operation error
    return date.toLocaleDateString('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
    });
};



/**
 * Debounce function to limit rapid invocations.
 * @param {Function} func - The function to debounce
 * @param {number} wait - Delay in milliseconds
 * @returns {Function} Debounced function
 */
const debounce = (func, wait = 300) => {
    let timeout;
    return (...args) => {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), wait);
    };
};

