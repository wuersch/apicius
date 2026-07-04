import { Route, Routes } from 'react-router'
import { AppChrome } from '@/components/AppChrome'
import { HomePage } from '@/pages/HomePage'
import { EditorPlaceholderPage } from '@/pages/EditorPlaceholderPage'

function App() {
  return (
    <div className="flex min-h-svh flex-col">
      <AppChrome />
      <main className="flex flex-1 flex-col">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/apis/:id" element={<EditorPlaceholderPage />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
