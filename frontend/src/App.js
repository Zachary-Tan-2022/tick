import "./App.css";
import { Routes, Route } from 'react-router-dom';

import { Navbar } from "./component/Navbar";
import { Footer } from "./component/Footer";
import { Home } from "./pages/Home";
import { NationalStadium } from "./pages/Seatmap/NationalStadium";

function App() {
  return (
    <div className="App">
      <Navbar />
      <div className="bg-black px-8">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/seatmap" element={<NationalStadium />} />
        </Routes>
      </div>
      <Footer />
    </div>
  );
}

export default App;
