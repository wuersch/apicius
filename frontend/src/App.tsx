import { Route, Routes } from 'react-router'
import { AppChrome } from '@/components/AppChrome'
import { HomePage } from '@/pages/HomePage'
import { EditorPage } from '@/pages/EditorPage'

function App() {
  return (
    <div className="flex min-h-svh flex-col">
      <AppChrome />
      <main className="flex flex-1 flex-col">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/apis/:id" element={<EditorPage />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
